package com.yuno.gateway.entity;

import com.yuno.gateway.enums.DeclineReason;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.PaymentStatus;
import com.yuno.gateway.enums.ProviderCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core Payment entity representing a single payment transaction.
 * 
 * Uses optimistic locking (@Version) to prevent concurrent status updates.
 * The idempotencyKey has a unique constraint to prevent duplicate payment creation.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_idempotency", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_payment_merchant", columnList = "merchantId"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_method", columnList = "paymentMethod"),
    @Index(name = "idx_payment_created", columnList = "createdAt")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private ProviderCode providerUsed;

    private String providerTransactionId;

    @Enumerated(EnumType.STRING)
    private DeclineReason declineReason;

    private String declineMessage;

    // Card details (masked for PCI compliance)
    private String maskedCardNumber;    // e.g., "****-****-****-4242"
    private String cardBrand;           // VISA, MASTERCARD, AMEX
    private String cardExpiryMonth;
    private String cardExpiryYear;

    // UPI details
    private String upiVpa;             // e.g., "user@upi"

    // Metadata as JSON string
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Description / order reference
    private String description;

    // Customer info
    private String customerEmail;
    private String customerName;

    // Retry tracking
    @Builder.Default
    private int attemptCount = 0;
    private int maxAttempts;

    // Routing attempts - each attempt to process this payment
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("attemptNumber ASC")
    private List<RoutingAttempt> routingAttempts = new ArrayList<>();

    // Optimistic locking
    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Add a routing attempt and maintain bidirectional relationship.
     */
    public void addRoutingAttempt(RoutingAttempt attempt) {
        routingAttempts.add(attempt);
        attempt.setPayment(this);
    }
}
