package com.yuno.gateway.repository;

import com.yuno.gateway.entity.RoutingAttempt;
import com.yuno.gateway.enums.ProviderCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoutingAttemptRepository extends JpaRepository<RoutingAttempt, UUID> {

    List<RoutingAttempt> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);

    @Query("SELECT COUNT(r) FROM RoutingAttempt r WHERE r.providerCode = :code AND r.createdAt BETWEEN :start AND :end")
    long countByProviderAndDateRange(@Param("code") ProviderCode code,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM RoutingAttempt r WHERE r.providerCode = :code AND r.success = true AND r.createdAt BETWEEN :start AND :end")
    long countSuccessByProviderAndDateRange(@Param("code") ProviderCode code,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT AVG(r.latencyMs) FROM RoutingAttempt r WHERE r.providerCode = :code AND r.createdAt BETWEEN :start AND :end")
    Double avgLatencyByProviderAndDateRange(@Param("code") ProviderCode code,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT r.latencyMs FROM RoutingAttempt r WHERE r.providerCode = :code AND r.createdAt BETWEEN :start AND :end ORDER BY r.latencyMs ASC")
    List<Long> findLatenciesByProviderAndDateRange(@Param("code") ProviderCode code,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);
}
