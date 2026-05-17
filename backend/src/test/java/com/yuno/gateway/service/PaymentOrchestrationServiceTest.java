package com.yuno.gateway.service;

import com.yuno.gateway.dto.request.CreatePaymentRequest;
import com.yuno.gateway.dto.response.PaymentResponse;
import com.yuno.gateway.entity.Payment;
import com.yuno.gateway.enums.*;
import com.yuno.gateway.exception.DuplicatePaymentException;
import com.yuno.gateway.provider.ProviderConnectorFactory;
import com.yuno.gateway.provider.ProviderAConnector;
import com.yuno.gateway.provider.ProviderResponse;
import com.yuno.gateway.repository.PaymentRepository;
import com.yuno.gateway.repository.TransactionMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the central PaymentOrchestrationService.
 * 
 * Tests cover:
 * - Happy path: successful card and UPI payments
 * - Idempotency: duplicate request returns cached response
 * - Failover: soft decline triggers retry to next provider
 * - Hard decline: no retry for insufficient funds
 * - Card masking: PCI compliance verification
 */
@ExtendWith(MockitoExtension.class)
class PaymentOrchestrationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private TransactionMetricRepository metricRepository;
    @Mock private IdempotencyService idempotencyService;
    @Mock private RoutingEngineService routingEngine;
    @Mock private ProviderConnectorFactory connectorFactory;
    @Mock private ComplianceService complianceService;
    @Mock private ApprovalRateService approvalRateService;
    @Mock private ProviderAConnector providerAConnector;

    @InjectMocks
    private PaymentOrchestrationService orchestrationService;

    private CreatePaymentRequest cardRequest;
    private CreatePaymentRequest upiRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orchestrationService, "maxRetries", 3);

        cardRequest = CreatePaymentRequest.builder()
            .merchantId("merchant_001")
            .amount(new BigDecimal("1500.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .cardNumber("4242424242424242")
            .cardExpiryMonth("12")
            .cardExpiryYear("2028")
            .cardCvv("123")
            .description("Test card payment")
            .customerEmail("test@example.com")
            .build();

        upiRequest = CreatePaymentRequest.builder()
            .merchantId("merchant_001")
            .amount(new BigDecimal("500.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.UPI)
            .upiVpa("user@paytm")
            .description("Test UPI payment")
            .build();
    }

    @Test
    @DisplayName("Should process CARD payment successfully through Provider A")
    void shouldProcessCardPaymentSuccessfully() {
        // Arrange
        String idempotencyKey = "idem-card-001";

        when(idempotencyService.checkDuplicate(idempotencyKey)).thenReturn(null);
        when(idempotencyService.acquireLock(idempotencyKey)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(routingEngine.resolveProviderChain(PaymentMethod.CARD))
            .thenReturn(List.of(ProviderCode.PROVIDER_A, ProviderCode.PROVIDER_B));
        when(connectorFactory.getConnector(ProviderCode.PROVIDER_A)).thenReturn(providerAConnector);
        when(providerAConnector.processPayment(any()))
            .thenReturn(ProviderResponse.success(ProviderCode.PROVIDER_A, "TXN-A-001", 120));

        // Act
        PaymentResponse response = orchestrationService.createPayment(cardRequest, idempotencyKey);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(ProviderCode.PROVIDER_A, response.getProviderUsed());
        assertEquals(1, response.getAttemptCount());
        assertTrue(response.getMaskedCardNumber().endsWith("4242"));
        assertFalse(response.getMaskedCardNumber().contains("424242424242"));
        assertEquals("VISA", response.getCardBrand());

        verify(idempotencyService).storeResponse(eq(idempotencyKey), any());
        verify(complianceService).recordPaymentCreation(anyString(), eq("merchant_001"), anyString());
        verify(approvalRateService).recordSuccess(ProviderCode.PROVIDER_A, PaymentMethod.CARD);
    }

    @Test
    @DisplayName("Should process UPI payment successfully through Provider B")
    void shouldProcessUpiPaymentSuccessfully() {
        String idempotencyKey = "idem-upi-001";
        var providerBMock = mock(com.yuno.gateway.provider.ProviderBConnector.class);

        when(idempotencyService.checkDuplicate(idempotencyKey)).thenReturn(null);
        when(idempotencyService.acquireLock(idempotencyKey)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(routingEngine.resolveProviderChain(PaymentMethod.UPI))
            .thenReturn(List.of(ProviderCode.PROVIDER_B));
        when(connectorFactory.getConnector(ProviderCode.PROVIDER_B)).thenReturn(providerBMock);
        when(providerBMock.processPayment(any()))
            .thenReturn(ProviderResponse.success(ProviderCode.PROVIDER_B, "TXN-B-001", 80));

        PaymentResponse response = orchestrationService.createPayment(upiRequest, idempotencyKey);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(ProviderCode.PROVIDER_B, response.getProviderUsed());
        assertEquals("user@paytm", response.getUpiVpa());
    }

    @Test
    @DisplayName("Should return cached response for duplicate idempotency key")
    void shouldReturnCachedResponseForDuplicate() {
        String idempotencyKey = "idem-duplicate-001";

        PaymentResponse cachedResponse = PaymentResponse.builder()
            .id(UUID.randomUUID())
            .status(PaymentStatus.SUCCESS)
            .merchantId("merchant_001")
            .build();

        when(idempotencyService.checkDuplicate(idempotencyKey)).thenReturn(cachedResponse);

        PaymentResponse response = orchestrationService.createPayment(cardRequest, idempotencyKey);

        assertSame(cachedResponse, response);
        verify(paymentRepository, never()).save(any());
        verify(routingEngine, never()).resolveProviderChain(any());
    }

    @Test
    @DisplayName("Should failover to Provider B when Provider A returns soft decline")
    void shouldFailoverOnSoftDecline() {
        String idempotencyKey = "idem-failover-001";
        var providerBMock = mock(com.yuno.gateway.provider.ProviderBConnector.class);

        when(idempotencyService.checkDuplicate(idempotencyKey)).thenReturn(null);
        when(idempotencyService.acquireLock(idempotencyKey)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(routingEngine.resolveProviderChain(PaymentMethod.CARD))
            .thenReturn(List.of(ProviderCode.PROVIDER_A, ProviderCode.PROVIDER_B));
        when(connectorFactory.getConnector(ProviderCode.PROVIDER_A)).thenReturn(providerAConnector);
        when(connectorFactory.getConnector(ProviderCode.PROVIDER_B)).thenReturn(providerBMock);

        // Provider A: soft decline (TIMEOUT - retryable)
        when(providerAConnector.processPayment(any()))
            .thenReturn(ProviderResponse.failure(ProviderCode.PROVIDER_A, DeclineReason.TIMEOUT, 200, "Timeout"));
        // Provider B: success
        when(providerBMock.processPayment(any()))
            .thenReturn(ProviderResponse.success(ProviderCode.PROVIDER_B, "TXN-B-FAILOVER", 100));

        PaymentResponse response = orchestrationService.createPayment(cardRequest, idempotencyKey);

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(ProviderCode.PROVIDER_B, response.getProviderUsed());
        assertEquals(2, response.getAttemptCount());

        verify(approvalRateService).recordFailure(ProviderCode.PROVIDER_A, PaymentMethod.CARD);
        verify(approvalRateService).recordSuccess(ProviderCode.PROVIDER_B, PaymentMethod.CARD);
    }

    @Test
    @DisplayName("Should NOT retry on hard decline (insufficient funds)")
    void shouldNotRetryOnHardDecline() {
        String idempotencyKey = "idem-hard-decline-001";

        when(idempotencyService.checkDuplicate(idempotencyKey)).thenReturn(null);
        when(idempotencyService.acquireLock(idempotencyKey)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(routingEngine.resolveProviderChain(PaymentMethod.CARD))
            .thenReturn(List.of(ProviderCode.PROVIDER_A, ProviderCode.PROVIDER_B));
        when(connectorFactory.getConnector(ProviderCode.PROVIDER_A)).thenReturn(providerAConnector);

        // Provider A: HARD decline (insufficient funds — NOT retryable)
        when(providerAConnector.processPayment(any()))
            .thenReturn(ProviderResponse.failure(ProviderCode.PROVIDER_A,
                DeclineReason.INSUFFICIENT_FUNDS, 150, "Insufficient funds"));

        PaymentResponse response = orchestrationService.createPayment(cardRequest, idempotencyKey);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals(DeclineReason.INSUFFICIENT_FUNDS, response.getDeclineReason());
        assertEquals(1, response.getAttemptCount());

        // Verify Provider B was NEVER called
        verify(connectorFactory, never()).getConnector(ProviderCode.PROVIDER_B);
    }

    @Test
    @DisplayName("Should mask card number in response (PCI compliance)")
    void shouldMaskCardNumber() {
        String idempotencyKey = "idem-mask-001";

        when(idempotencyService.checkDuplicate(idempotencyKey)).thenReturn(null);
        when(idempotencyService.acquireLock(idempotencyKey)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(routingEngine.resolveProviderChain(PaymentMethod.CARD))
            .thenReturn(List.of(ProviderCode.PROVIDER_A));
        when(connectorFactory.getConnector(ProviderCode.PROVIDER_A)).thenReturn(providerAConnector);
        when(providerAConnector.processPayment(any()))
            .thenReturn(ProviderResponse.success(ProviderCode.PROVIDER_A, "TXN-001", 100));

        PaymentResponse response = orchestrationService.createPayment(cardRequest, idempotencyKey);

        // Card number must be masked
        assertNotNull(response.getMaskedCardNumber());
        assertTrue(response.getMaskedCardNumber().contains("****"));
        assertTrue(response.getMaskedCardNumber().endsWith("4242"));
        // Full card number must NOT appear
        assertFalse(response.getMaskedCardNumber().contains("4242424242424242"));
    }

    @Test
    @DisplayName("Should fail when concurrent lock not acquired")
    void shouldFailWhenLockNotAcquired() {
        String idempotencyKey = "idem-concurrent-001";

        when(idempotencyService.checkDuplicate(idempotencyKey)).thenReturn(null);
        when(idempotencyService.acquireLock(idempotencyKey)).thenReturn(false);

        assertThrows(DuplicatePaymentException.class,
            () -> orchestrationService.createPayment(cardRequest, idempotencyKey));

        verify(paymentRepository, never()).save(any());
    }
}
