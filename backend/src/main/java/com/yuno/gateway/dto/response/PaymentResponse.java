package com.yuno.gateway.dto.response;

import com.yuno.gateway.enums.DeclineReason;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.PaymentStatus;
import com.yuno.gateway.enums.ProviderCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Payment response DTO - ensures sensitive data is never exposed.
 * Card numbers are always masked, CVV is never included.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Schema(description = "Payment transaction response")
public class PaymentResponse {

    @Schema(description = "Unique payment identifier")
    private UUID id;

    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private ProviderCode providerUsed;
    private String providerTransactionId;
    private DeclineReason declineReason;
    private String declineMessage;

    // Masked card info (PCI compliant)
    private String maskedCardNumber;
    private String cardBrand;

    // UPI info
    private String upiVpa;

    private String description;
    private String customerEmail;
    private String customerName;
    private String metadata;

    private int attemptCount;
    private List<RoutingAttemptResponse> routingAttempts;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class RoutingAttemptResponse {
        private int attemptNumber;
        private ProviderCode providerCode;
        private boolean success;
        private DeclineReason declineReason;
        private long latencyMs;
        private String errorMessage;
        private LocalDateTime createdAt;
    }
}
