package com.yuno.gateway.service;

import com.yuno.gateway.dto.request.CreatePaymentRequest;
import com.yuno.gateway.dto.response.PaymentResponse;
import com.yuno.gateway.entity.Payment;
import com.yuno.gateway.entity.RoutingAttempt;
import com.yuno.gateway.entity.TransactionMetric;
import com.yuno.gateway.enums.*;
import com.yuno.gateway.exception.*;
import com.yuno.gateway.provider.ProviderConnector;
import com.yuno.gateway.provider.ProviderConnectorFactory;
import com.yuno.gateway.provider.ProviderResponse;
import com.yuno.gateway.repository.PaymentRepository;
import com.yuno.gateway.repository.TransactionMetricRepository;
import com.yuno.gateway.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment Orchestration Service - The Central Nervous System
 * 
 * This is the main orchestrator that coordinates the entire payment flow:
 * 
 * 1. VALIDATE → Request validation + idempotency check
 * 2. CREATE   → Persist payment with INITIATED status
 * 3. ROUTE    → Resolve provider chain via Routing Engine
 * 4. EXECUTE  → Send to provider, handle response
 * 5. RETRY    → On soft decline, try next provider in chain
 * 6. RECORD   → Persist result, record metrics, audit log
 * 7. RESPOND  → Return standardized response
 * 
 * Key design decisions:
 * - Idempotency checked BEFORE any DB write (Redis → fast)
 * - Payment status transitions enforced via optimistic locking
 * - Retry only on soft declines (timeout, network error)
 * - Hard declines (insufficient funds) immediately fail
 * - All provider interactions are recorded as RoutingAttempts
 * - TransactionMetrics written for analytics aggregation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrchestrationService {

    private final PaymentRepository paymentRepository;
    private final TransactionMetricRepository metricRepository;
    private final IdempotencyService idempotencyService;
    private final RoutingEngineService routingEngine;
    private final ProviderConnectorFactory connectorFactory;
    private final ComplianceService complianceService;
    private final ApprovalRateService approvalRateService;

    @Value("${app.routing.max-retries:3}")
    private int maxRetries;

    /**
     * Process a new payment request through the orchestration pipeline.
     *
     * @param request        The payment creation request
     * @param idempotencyKey Client-provided idempotency key
     * @return Payment response with status and provider details
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        log.info("=== Payment Orchestration START === method={}, amount={} {}, idempotencyKey={}",
                 request.getPaymentMethod(), request.getAmount(), request.getCurrency(), idempotencyKey);

        // Step 1: Idempotency check (Redis-first)
        PaymentResponse cached = idempotencyService.checkDuplicate(idempotencyKey);
        if (cached != null) {
            log.info("Returning cached idempotent response for key: {}", idempotencyKey);
            return cached;
        }

        // Acquire processing lock
        if (!idempotencyService.acquireLock(idempotencyKey)) {
            throw new DuplicatePaymentException(
                "Payment is currently being processed for idempotency key: " + idempotencyKey, null);
        }

        try {
            // Step 2: Create payment record
            Payment payment = buildPayment(request, idempotencyKey);
            payment = paymentRepository.save(payment);
            log.info("Payment created: id={}, status=INITIATED", payment.getId());

            // Audit: Payment creation
            complianceService.recordPaymentCreation(
                payment.getId().toString(), request.getMerchantId(),
                String.format("amount=%s %s, method=%s", request.getAmount(), 
                              request.getCurrency(), request.getPaymentMethod()));

            // Step 3: Resolve provider chain
            List<ProviderCode> providerChain = routingEngine.resolveProviderChain(request.getPaymentMethod());
            log.info("Provider chain resolved: {}", providerChain);

            // Step 4: Execute payment through provider chain (with retry/failover)
            payment = executeWithFailover(payment, request, providerChain);

            // Step 5: Build response and cache for idempotency
            PaymentResponse response = toResponse(payment);
            idempotencyService.storeResponse(idempotencyKey, response);

            log.info("=== Payment Orchestration END === id={}, status={}, provider={}, attempts={}",
                     payment.getId(), payment.getStatus(), payment.getProviderUsed(), payment.getAttemptCount());

            return response;

        } catch (Exception e) {
            idempotencyService.releaseLock(idempotencyKey);
            throw e;
        }
    }

    /**
     * Execute payment with automatic failover across the provider chain.
     * 
     * For each provider in the chain:
     * 1. Send payment request
     * 2. If success → return
     * 3. If soft decline → record attempt, try next provider
     * 4. If hard decline → record attempt, fail immediately
     */
    private Payment executeWithFailover(Payment payment, CreatePaymentRequest request,
                                         List<ProviderCode> providerChain) {
        int attemptNumber = 0;
        ProviderResponse lastResponse = null;

        for (ProviderCode providerCode : providerChain) {
            if (attemptNumber >= maxRetries) {
                log.warn("Max retry attempts ({}) reached for payment: {}", maxRetries, payment.getId());
                break;
            }

            attemptNumber++;
            payment.setStatus(PaymentStatus.PROCESSING);
            payment.setAttemptCount(attemptNumber);

            log.info("Attempt #{} via {} for payment {}", attemptNumber, providerCode, payment.getId());

            try {
                ProviderConnector connector = connectorFactory.getConnector(providerCode);
                lastResponse = connector.processPayment(request);

                // Record this attempt
                RoutingAttempt attempt = RoutingAttempt.builder()
                    .attemptNumber(attemptNumber)
                    .providerCode(providerCode)
                    .success(lastResponse.isSuccess())
                    .providerTransactionId(lastResponse.getProviderTransactionId())
                    .declineReason(lastResponse.getDeclineReason())
                    .latencyMs(lastResponse.getLatencyMs())
                    .rawProviderResponse(lastResponse.getRawResponse())
                    .errorMessage(lastResponse.getErrorMessage())
                    .build();
                payment.addRoutingAttempt(attempt);

                // Record metric for analytics
                recordMetric(payment, providerCode, lastResponse, attemptNumber, attemptNumber > 1);

                if (lastResponse.isSuccess()) {
                    // SUCCESS — update payment and return
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setProviderUsed(providerCode);
                    payment.setProviderTransactionId(lastResponse.getProviderTransactionId());

                    complianceService.recordStatusChange(
                        payment.getId().toString(), "PROCESSING", "SUCCESS", payment.getMerchantId());

                    // Update approval rate tracking
                    approvalRateService.recordSuccess(providerCode, payment.getPaymentMethod());

                    payment = paymentRepository.save(payment);
                    return payment;
                }

                // DECLINED — check if retryable
                if (lastResponse.getDeclineReason() != null && !lastResponse.getDeclineReason().isRetryable()) {
                    // Hard decline — do NOT retry
                    log.warn("Hard decline from {} ({}). Not retrying.", 
                             providerCode, lastResponse.getDeclineReason());
                    
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setProviderUsed(providerCode);
                    payment.setDeclineReason(lastResponse.getDeclineReason());
                    payment.setDeclineMessage(lastResponse.getErrorMessage());

                    complianceService.recordStatusChange(
                        payment.getId().toString(), "PROCESSING", "FAILED", payment.getMerchantId());
                    
                    approvalRateService.recordFailure(providerCode, payment.getPaymentMethod());
                    
                    payment = paymentRepository.save(payment);
                    return payment;
                }

                // Soft decline — retryable, try next provider
                log.info("Soft decline from {} ({}). Trying next provider in chain...",
                         providerCode, lastResponse.getDeclineReason());
                
                payment.setStatus(PaymentStatus.RETRY);
                complianceService.recordRetry(payment.getId().toString(), payment.getMerchantId(),
                    String.format("Attempt #%d failed via %s: %s", attemptNumber, providerCode, 
                                  lastResponse.getDeclineReason()));
                
                approvalRateService.recordFailure(providerCode, payment.getPaymentMethod());

            } catch (ProviderUnavailableException e) {
                log.error("Provider {} unavailable: {}", providerCode, e.getMessage());
                // Record failed attempt and continue to next provider
                RoutingAttempt attempt = RoutingAttempt.builder()
                    .attemptNumber(attemptNumber)
                    .providerCode(providerCode)
                    .success(false)
                    .declineReason(DeclineReason.NETWORK_ERROR)
                    .errorMessage(e.getMessage())
                    .latencyMs(0)
                    .build();
                payment.addRoutingAttempt(attempt);
            }
        }

        // All providers exhausted — mark as failed
        payment.setStatus(PaymentStatus.FAILED);
        if (lastResponse != null) {
            payment.setDeclineReason(lastResponse.getDeclineReason());
            payment.setDeclineMessage("All providers in failover chain exhausted");
        }

        complianceService.recordStatusChange(
            payment.getId().toString(), "RETRY", "FAILED", payment.getMerchantId());

        payment = paymentRepository.save(payment);
        return payment;
    }

    /**
     * Fetch a payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        return toResponse(payment);
    }

    /**
     * List payments with pagination and optional filters.
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(String merchantId, PaymentStatus status, Pageable pageable) {
        Page<Payment> payments;

        if (merchantId != null && status != null) {
            payments = paymentRepository.findByMerchantIdAndStatus(merchantId, status, pageable);
        } else if (merchantId != null) {
            payments = paymentRepository.findByMerchantId(merchantId, pageable);
        } else if (status != null) {
            payments = paymentRepository.findByStatus(status, pageable);
        } else {
            payments = paymentRepository.findAll(pageable);
        }

        return payments.map(this::toResponse);
    }

    /**
     * Initiate a refund for a successful payment.
     */
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Can only refund successful payments. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUND_INITIATED);
        
        // Call provider refund
        ProviderConnector connector = connectorFactory.getConnector(payment.getProviderUsed());
        ProviderResponse refundResponse = connector.refund(payment.getProviderTransactionId(), payment.getAmount());

        if (refundResponse.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            complianceService.recordStatusChange(
                paymentId.toString(), "SUCCESS", "REFUNDED", payment.getMerchantId());
        } else {
            payment.setStatus(PaymentStatus.SUCCESS); // Revert
            complianceService.recordAudit("PAYMENT", paymentId.toString(), "REFUND_FAILED",
                null, refundResponse.getErrorMessage(), payment.getMerchantId());
        }

        payment = paymentRepository.save(payment);
        return toResponse(payment);
    }

    // ======================== Private helpers ========================

    private Payment buildPayment(CreatePaymentRequest request, String idempotencyKey) {
        Payment.PaymentBuilder builder = Payment.builder()
            .merchantId(request.getMerchantId())
            .idempotencyKey(idempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency().toUpperCase())
            .paymentMethod(request.getPaymentMethod())
            .status(PaymentStatus.INITIATED)
            .description(request.getDescription())
            .customerEmail(request.getCustomerEmail())
            .customerName(request.getCustomerName())
            .metadata(request.getMetadata())
            .maxAttempts(maxRetries);

        // Mask card details (PCI compliance — never store raw PAN)
        if (request.getPaymentMethod() == PaymentMethod.CARD && request.getCardNumber() != null) {
            builder.maskedCardNumber(CardMaskingUtil.mask(request.getCardNumber()));
            builder.cardBrand(CardMaskingUtil.detectBrand(request.getCardNumber()));
            builder.cardExpiryMonth(request.getCardExpiryMonth());
            builder.cardExpiryYear(request.getCardExpiryYear());
        }

        if (request.getPaymentMethod() == PaymentMethod.UPI) {
            builder.upiVpa(request.getUpiVpa());
        }

        return builder.build();
    }

    private void recordMetric(Payment payment, ProviderCode providerCode,
                               ProviderResponse response, int attemptNumber, boolean wasRetry) {
        try {
            TransactionMetric metric = TransactionMetric.builder()
                .paymentId(payment.getId())
                .providerCode(providerCode)
                .paymentMethod(payment.getPaymentMethod())
                .status(response.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .latencyMs(response.getLatencyMs())
                .attemptNumber(attemptNumber)
                .declineReason(response.getDeclineReason())
                .wasRetried(wasRetry)
                .wasFailover(attemptNumber > 1)
                .build();
            metricRepository.save(metric);
        } catch (Exception e) {
            log.error("Failed to record transaction metric: {}", e.getMessage());
        }
    }

    /**
     * Convert Payment entity to response DTO.
     * Ensures sensitive data is never leaked.
     */
    private PaymentResponse toResponse(Payment payment) {
        List<PaymentResponse.RoutingAttemptResponse> attempts = payment.getRoutingAttempts().stream()
            .map(a -> PaymentResponse.RoutingAttemptResponse.builder()
                .attemptNumber(a.getAttemptNumber())
                .providerCode(a.getProviderCode())
                .success(a.isSuccess())
                .declineReason(a.getDeclineReason())
                .latencyMs(a.getLatencyMs())
                .errorMessage(a.getErrorMessage())
                .createdAt(a.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return PaymentResponse.builder()
            .id(payment.getId())
            .merchantId(payment.getMerchantId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .paymentMethod(payment.getPaymentMethod())
            .status(payment.getStatus())
            .providerUsed(payment.getProviderUsed())
            .providerTransactionId(payment.getProviderTransactionId())
            .declineReason(payment.getDeclineReason())
            .declineMessage(payment.getDeclineMessage())
            .maskedCardNumber(payment.getMaskedCardNumber())
            .cardBrand(payment.getCardBrand())
            .upiVpa(payment.getUpiVpa())
            .description(payment.getDescription())
            .customerEmail(payment.getCustomerEmail())
            .customerName(payment.getCustomerName())
            .metadata(payment.getMetadata())
            .attemptCount(payment.getAttemptCount())
            .routingAttempts(attempts)
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }
}
