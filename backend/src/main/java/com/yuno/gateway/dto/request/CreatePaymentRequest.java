package com.yuno.gateway.dto.request;

import com.yuno.gateway.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new payment.
 * Validates all input before entering the orchestration pipeline.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating a new payment transaction")
public class CreatePaymentRequest {

    @NotBlank(message = "Merchant ID is required")
    @Schema(description = "Unique merchant identifier", example = "merchant_001")
    private String merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
    @Schema(description = "Payment amount", example = "1500.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Schema(description = "ISO 4217 currency code", example = "INR")
    private String currency;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment method type", example = "CARD")
    private PaymentMethod paymentMethod;

    // Card details (required when paymentMethod = CARD)
    @Schema(description = "Card number (will be masked after processing)", example = "4242424242424242")
    private String cardNumber;

    @Schema(description = "Card expiry month", example = "12")
    private String cardExpiryMonth;

    @Schema(description = "Card expiry year", example = "2028")
    private String cardExpiryYear;

    @Schema(description = "Card CVV (not stored)", example = "123")
    private String cardCvv;

    @Schema(description = "Cardholder name", example = "John Doe")
    private String cardHolderName;

    // UPI details (required when paymentMethod = UPI)
    @Schema(description = "UPI Virtual Payment Address", example = "user@paytm")
    private String upiVpa;

    // Optional metadata
    @Schema(description = "Payment description / order reference", example = "Order #12345")
    private String description;

    @Email(message = "Invalid email format")
    @Schema(description = "Customer email for receipts", example = "customer@example.com")
    private String customerEmail;

    @Schema(description = "Customer name", example = "John Doe")
    private String customerName;

    @Schema(description = "Additional metadata as JSON string", example = "{\"orderId\": \"ORD-001\"}")
    private String metadata;
}
