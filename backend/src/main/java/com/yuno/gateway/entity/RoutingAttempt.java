package com.yuno.gateway.entity;

import com.yuno.gateway.enums.DeclineReason;
import com.yuno.gateway.enums.ProviderCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records each individual attempt to process a payment through a provider.
 * A single payment may have multiple routing attempts due to retry/failover.
 * 
 * This is critical for:
 * - Audit trail of all provider interactions
 * - Approval rate calculation per provider
 * - Latency tracking and performance analytics
 */
@Entity
@Table(name = "routing_attempts", indexes = {
    @Index(name = "idx_attempt_payment", columnList = "payment_id"),
    @Index(name = "idx_attempt_provider", columnList = "providerCode"),
    @Index(name = "idx_attempt_created", columnList = "createdAt")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoutingAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderCode providerCode;

    @Column(nullable = false)
    private boolean success;

    private String providerTransactionId;

    @Enumerated(EnumType.STRING)
    private DeclineReason declineReason;

    private String rawProviderResponse;

    // Performance metrics
    private long latencyMs;

    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
