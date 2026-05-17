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
 * Simulated Card Payment Provider (Provider A - "CardPay Global").
 * 
 * Simulates realistic card processing behavior:
 * - Configurable success rate (default 85%)
 * - Random latency between 50-300ms
 * - Realistic decline reasons (insufficient funds, expired card, etc.)
 * - Card brand detection from BIN
 * - Health toggle for testing failover scenarios
 */
@Component
@Slf4j
public class ProviderAConnector implements ProviderConnector {

    private final Random random = new Random();
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    @Value("${app.providers.provider-a.success-rate:0.85}")
    private double successRate;

    @Value("${app.providers.provider-a.min-latency-ms:50}")
    private int minLatency;

    @Value("${app.providers.provider-a.max-latency-ms:300}")
    private int maxLatency;

    // Card decline reasons with weighted probability
    private static final DeclineReason[] CARD_DECLINE_REASONS = {
        DeclineReason.INSUFFICIENT_FUNDS,   // 35%
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.INSUFFICIENT_FUNDS,
        DeclineReason.CARD_EXPIRED,         // 20%
        DeclineReason.CARD_EXPIRED,
        DeclineReason.CARD_EXPIRED,
        DeclineReason.CARD_EXPIRED,
        DeclineReason.DO_NOT_HONOR,         // 15%
        DeclineReason.DO_NOT_HONOR,
        DeclineReason.DO_NOT_HONOR,
        DeclineReason.TIMEOUT,              // 15% (soft - retryable)
        DeclineReason.TIMEOUT,
        DeclineReason.TIMEOUT,
        DeclineReason.FRAUD_SUSPECTED,      // 10%
        DeclineReason.FRAUD_SUSPECTED,
        DeclineReason.NETWORK_ERROR         // 5% (soft - retryable)
    };

    @Override
    public ProviderResponse processPayment(CreatePaymentRequest request) {
        log.info("[Provider A] Processing CARD payment: amount={} {}", request.getAmount(), request.getCurrency());

        long latency = simulateLatency();

        if (!healthy.get()) {
            log.warn("[Provider A] Provider is DOWN - returning network error");
            return ProviderResponse.failure(ProviderCode.PROVIDER_A, 
                DeclineReason.NETWORK_ERROR, latency, "Provider A is currently unavailable");
        }

        // Simulate processing
        if (random.nextDouble() < successRate) {
            String txnId = "TXN-A-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            log.info("[Provider A] Payment APPROVED: txnId={}, latency={}ms", txnId, latency);
            return ProviderResponse.success(ProviderCode.PROVIDER_A, txnId, latency);
        } else {
            DeclineReason reason = CARD_DECLINE_REASONS[random.nextInt(CARD_DECLINE_REASONS.length)];
            log.warn("[Provider A] Payment DECLINED: reason={}, latency={}ms", reason, latency);
            return ProviderResponse.failure(ProviderCode.PROVIDER_A, reason, latency, reason.getDescription());
        }
    }

    @Override
    public ProviderResponse checkStatus(String providerTransactionId) {
        long latency = simulateLatency();
        return ProviderResponse.success(ProviderCode.PROVIDER_A, providerTransactionId, latency);
    }

    @Override
    public ProviderResponse refund(String providerTransactionId, BigDecimal amount) {
        log.info("[Provider A] Processing refund: txnId={}, amount={}", providerTransactionId, amount);
        long latency = simulateLatency();
        
        if (random.nextDouble() < 0.95) { // 95% refund success rate
            String refundId = "REF-A-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            return ProviderResponse.success(ProviderCode.PROVIDER_A, refundId, latency);
        }
        return ProviderResponse.failure(ProviderCode.PROVIDER_A, DeclineReason.UNKNOWN, latency, "Refund failed");
    }

    @Override
    public ProviderCode getProviderCode() {
        return ProviderCode.PROVIDER_A;
    }

    @Override
    public boolean isHealthy() {
        return healthy.get();
    }

    /**
     * Toggle provider health (for testing failover scenarios).
     */
    public void setHealthy(boolean isHealthy) {
        this.healthy.set(isHealthy);
        log.info("[Provider A] Health status changed to: {}", isHealthy ? "HEALTHY" : "DOWN");
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
