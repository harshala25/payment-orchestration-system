package com.yuno.gateway.entity;

import com.yuno.gateway.enums.DeclineReason;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.PaymentStatus;
import com.yuno.gateway.enums.ProviderCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denormalized transaction metric for fast analytics queries.
 * 
 * While routing_attempts stores the raw data, this table is optimized
 * for aggregation queries (approval rates, volume, latency percentiles).
 * 
 * This follows the CQRS-lite pattern: write to routing_attempts,
 * read/aggregate from transaction_metrics.
 */
@Entity
@Table(name = "transaction_metrics", indexes = {
    @Index(name = "idx_metric_provider", columnList = "providerCode"),
    @Index(name = "idx_metric_method", columnList = "paymentMethod"),
    @Index(name = "idx_metric_status", columnList = "status"),
    @Index(name = "idx_metric_timestamp", columnList = "timestamp"),
    @Index(name = "idx_metric_provider_time", columnList = "providerCode, timestamp")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransactionMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderCode providerCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private BigDecimal amount;
    private String currency;

    private long latencyMs;
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    private DeclineReason declineReason;

    private boolean wasRetried;
    private boolean wasFailover;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
