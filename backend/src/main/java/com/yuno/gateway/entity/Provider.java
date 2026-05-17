package com.yuno.gateway.entity;

import com.yuno.gateway.enums.ProviderCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Provider configuration entity.
 * 
 * Tracks provider health, capability, and configuration.
 * The isEnabled flag can be toggled manually or automatically
 * when approval rates drop below configured thresholds.
 */
@Entity
@Table(name = "providers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ProviderCode code;

    @Builder.Default
    private boolean isEnabled = true;

    @Builder.Default
    private String healthStatus = "HEALTHY"; // HEALTHY, DEGRADED, DOWN

    private String supportedMethods; // Comma-separated: "CARD,WALLET"

    private String baseUrl;
    private int timeoutMs;

    // Real-time metrics cache
    @Builder.Default
    private double currentApprovalRate = 100.0;
    
    @Builder.Default
    private long totalTransactions = 0;
    
    @Builder.Default
    private long successfulTransactions = 0;

    private double averageLatencyMs;

    private LocalDateTime lastHealthCheck;
    private LocalDateTime lastDowntime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
