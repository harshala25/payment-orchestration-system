package com.yuno.gateway.repository;

import com.yuno.gateway.entity.TransactionMetric;
import com.yuno.gateway.enums.DeclineReason;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.PaymentStatus;
import com.yuno.gateway.enums.ProviderCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionMetricRepository extends JpaRepository<TransactionMetric, UUID> {

    long countByProviderCodeAndTimestampBetween(ProviderCode code, LocalDateTime start, LocalDateTime end);

    long countByProviderCodeAndStatusAndTimestampBetween(ProviderCode code, PaymentStatus status,
                                                         LocalDateTime start, LocalDateTime end);

    long countByPaymentMethodAndTimestampBetween(PaymentMethod method, LocalDateTime start, LocalDateTime end);

    long countByPaymentMethodAndStatusAndTimestampBetween(PaymentMethod method, PaymentStatus status,
                                                          LocalDateTime start, LocalDateTime end);

    @Query("SELECT m.declineReason, COUNT(m) FROM TransactionMetric m " +
           "WHERE m.status = 'FAILED' AND m.declineReason IS NOT NULL " +
           "AND m.timestamp BETWEEN :start AND :end " +
           "GROUP BY m.declineReason ORDER BY COUNT(m) DESC")
    List<Object[]> countByDeclineReasonGrouped(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT AVG(m.latencyMs) FROM TransactionMetric m " +
           "WHERE m.providerCode = :code AND m.timestamp BETWEEN :start AND :end")
    Double avgLatencyByProvider(@Param("code") ProviderCode code,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndTimestampBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(m.amount) FROM TransactionMetric m WHERE m.timestamp BETWEEN :start AND :end")
    java.math.BigDecimal sumVolumeByDateRange(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
}
