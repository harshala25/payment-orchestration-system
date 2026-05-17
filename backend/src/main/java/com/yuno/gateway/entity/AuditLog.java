package com.yuno.gateway.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit log for compliance tracking.
 * 
 * Records all state changes, API calls, and security events.
 * Supports PCI-DSS requirement for comprehensive audit trails.
 * 
 * Retention is configurable via app.compliance.audit-retention-days.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_merchant", columnList = "merchantId")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String entityType; // PAYMENT, ROUTING_RULE, PROVIDER

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String action; // CREATE, STATUS_CHANGE, RETRY, REFUND, CONFIG_UPDATE

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    private String merchantId;
    private String performedBy; // API key or system
    private String ipAddress;
    private String userAgent;

    @Column(nullable = false)
    private String traceId; // Request correlation ID

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
