package com.yuno.gateway.entity;

import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.ProviderCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Defines routing rules that map payment methods to providers.
 * 
 * Rules are evaluated in priority order. Multiple rules can exist for
 * the same payment method to support failover chains:
 *   Priority 1: CARD → PROVIDER_A (weight 100%)
 *   Priority 2: CARD → PROVIDER_B (weight 100%, failover)
 * 
 * Weight-based routing allows A/B testing:
 *   Priority 1: CARD → PROVIDER_A (weight 70%)
 *   Priority 1: CARD → PROVIDER_C (weight 30%)
 */
@Entity
@Table(name = "routing_rules", indexes = {
    @Index(name = "idx_rule_method", columnList = "paymentMethod"),
    @Index(name = "idx_rule_active", columnList = "isActive")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderCode providerCode;

    /**
     * Lower number = higher priority.
     * Rules with same priority use weight-based selection.
     */
    @Builder.Default
    private int priority = 1;

    /**
     * Weight for load distribution (0-100).
     * When multiple rules share the same priority, traffic is distributed by weight.
     */
    @Builder.Default
    private int weight = 100;

    @Builder.Default
    private boolean isActive = true;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
