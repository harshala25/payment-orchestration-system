package com.yuno.gateway.provider;

import com.yuno.gateway.dto.request.CreatePaymentRequest;
import com.yuno.gateway.enums.DeclineReason;
import com.yuno.gateway.enums.ProviderCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulated UPI Payment Provider (Provider B - "UPI Direct Connect").
 * 
 * Simulates realistic UPI processing behavior:
 * - Configurable success rate (default 90%)
 * - Lower latency than card (30-200ms) - UPI is typically faster
 * - UPI-specific decline reasons (VPA not found, bank unavailable)
 * - VPA format validation
 */
@Component
@Slf4j
public class ProviderBConnector implements ProviderConnector {

    private final Random random = new Random();
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    @Value("${app.providers.provider-b.success-rate:0.90}")
    private double successRate;

    @Value("${app.providers.provider-b.min-latency-ms:30}")
    private int minLatency;

    @Value("${app.providers.provider-b.max-latency-ms:200}")
    private int maxLatency;

    // UPI decline reasons with weighted probability
    private static final DeclineReason[] UPI_DECLINE_REASONS = {
        DeclineReason.INSUFFICIENT_FUNDS,   // 30%
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.VPA_NOT_FOUND,        // 20%
        DeclineReason.VPA_NOT_FOUND,
        DeclineReason.VPA_NOT_FOUND,
        DeclineReason.VPA_NOT_FOUND,
        DeclineReason.TIMEOUT,              // 25% (soft - retryable)
        DeclineReason.TIMEOUT,
        DeclineReason.TIMEOUT,
        DeclineReason.TIMEOUT,
        DeclineReason.TIMEOUT,
        DeclineReason.BANK_UNAVAILABLE,     // 20% (soft - retryable)
        DeclineReason.BANK_UNAVAILABLE,
        DeclineReason.BANK_UNAVAILABLE,
        DeclineReason.BANK_UNAVAILABLE,
        DeclineReason.NETWORK_ERROR         // 5% (soft - retryable)
    };

    @Override
    public ProviderResponse processPayment(CreatePaymentRequest request) {
        log.info("[Provider B] Processing UPI payment: amount={} {}, vpa={}", 
                 request.getAmount(), request.getCurrency(), request.getUpiVpa());

        long latency = simulateLatency();

        if (!healthy.get()) {
            log.warn("[Provider B] Provider is DOWN - returning network error");
            return ProviderResponse.failure(ProviderCode.PROVIDER_B,
                DeclineReason.NETWORK_ERROR, latency, "Provider B is currently unavailable");
        }

        // Validate VPA format
        if (request.getUpiVpa() != null && !request.getUpiVpa().contains("@")) {
            return ProviderResponse.failure(ProviderCode.PROVIDER_B,
                DeclineReason.VPA_NOT_FOUND, latency, "Invalid VPA format");
        }

        // Simulate processing
        if (random.nextDouble() < successRate) {
            String txnId = "TXN-B-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            log.info("[Provider B] Payment APPROVED: txnId={}, latency={}ms", txnId, latency);
            return ProviderResponse.success(ProviderCode.PROVIDER_B, txnId, latency);
        } else {
            DeclineReason reason = UPI_DECLINE_REASONS[random.nextInt(UPI_DECLINE_REASONS.length)];
            log.warn("[Provider B] Payment DECLINED: reason={}, latency={}ms", reason, latency);
            return ProviderResponse.failure(ProviderCode.PROVIDER_B, reason, latency, reason.getDescription());
        }
    }

    @Override
    public ProviderResponse checkStatus(String providerTransactionId) {
        long latency = simulateLatency();
        return ProviderResponse.success(ProviderCode.PROVIDER_B, providerTransactionId, latency);
    }

    @Override
    public ProviderResponse refund(String providerTransactionId, BigDecimal amount) {
        log.info("[Provider B] Processing UPI refund: txnId={}, amount={}", providerTransactionId, amount);
        long latency = simulateLatency();

        if (random.nextDouble() < 0.95) {
            String refundId = "REF-B-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            return ProviderResponse.success(ProviderCode.PROVIDER_B, refundId, latency);
        }
        return ProviderResponse.failure(ProviderCode.PROVIDER_B, DeclineReason.UNKNOWN, latency, "Refund failed");
    }

    @Override
    public ProviderCode getProviderCode() {
        return ProviderCode.PROVIDER_B;
    }

    @Override
    public boolean isHealthy() {
        return healthy.get();
    }

    public void setHealthy(boolean isHealthy) {
        this.healthy.set(isHealthy);
        log.info("[Provider B] Health status changed to: {}", isHealthy ? "HEALTHY" : "DOWN");
    }

    private long simulateLatency() {
        int latency = minLatency + random.nextInt(maxLatency - minLatency + 1);
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return latency;
    }
}
