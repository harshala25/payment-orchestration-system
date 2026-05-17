package com.yuno.gateway.service;

import com.yuno.gateway.dto.response.DashboardResponse;
import com.yuno.gateway.entity.Provider;
import com.yuno.gateway.enums.*;
import com.yuno.gateway.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service - Core Pillar #5
 * Aggregates transaction data for dashboard, reports, and trend analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final TransactionMetricRepository metricRepo;
    private final RoutingAttemptRepository attemptRepo;
    private final ProviderRepository providerRepo;

    public DashboardResponse getDashboard(int hoursBack) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hoursBack);

        long total = metricRepo.countByTimestampBetween(start, end);
        long success = metricRepo.countByStatusAndTimestampBetween(PaymentStatus.SUCCESS, start, end);
        long failed = total - success;
        double successRate = total > 0 ? (double) success / total * 100 : 0;
        BigDecimal volume = metricRepo.sumVolumeByDateRange(start, end);

        // Payment method breakdown
        List<DashboardResponse.PaymentMethodStats> methodStats = new ArrayList<>();
        for (PaymentMethod m : PaymentMethod.values()) {
            long mTotal = metricRepo.countByPaymentMethodAndTimestampBetween(m, start, end);
            if (mTotal == 0) continue;
            long mSuccess = metricRepo.countByPaymentMethodAndStatusAndTimestampBetween(
                m, PaymentStatus.SUCCESS, start, end);
            methodStats.add(DashboardResponse.PaymentMethodStats.builder()
                .paymentMethod(m.name())
                .totalCount(mTotal).successCount(mSuccess)
                .approvalRate(mTotal > 0 ? (double) mSuccess / mTotal * 100 : 0)
                .build());
        }

        // Provider breakdown
        List<DashboardResponse.ProviderStats> providerStats = new ArrayList<>();
        for (ProviderCode c : ProviderCode.values()) {
            Provider p = providerRepo.findByCode(c).orElse(null);
            long pTotal = attemptRepo.countByProviderAndDateRange(c, start, end);
            if (pTotal == 0 && p == null) continue;
            long pSuccess = attemptRepo.countSuccessByProviderAndDateRange(c, start, end);
            Double avgLat = attemptRepo.avgLatencyByProviderAndDateRange(c, start, end);
            
            // Calculate percentiles
            List<Long> latencies = attemptRepo.findLatenciesByProviderAndDateRange(c, start, end);
            double p95 = calculatePercentile(latencies, 95);
            double p99 = calculatePercentile(latencies, 99);

            providerStats.add(DashboardResponse.ProviderStats.builder()
                .providerCode(c.name())
                .providerName(p != null ? p.getName() : c.name())
                .healthStatus(p != null ? p.getHealthStatus() : "UNKNOWN")
                .isEnabled(p != null && p.isEnabled())
                .totalCount(pTotal).successCount(pSuccess)
                .approvalRate(pTotal > 0 ? (double) pSuccess / pTotal * 100 : 0)
                .avgLatencyMs(avgLat != null ? avgLat : 0)
                .p95LatencyMs(p95).p99LatencyMs(p99)
                .build());
        }

        // Top decline reasons
        List<Object[]> declineData = metricRepo.countByDeclineReasonGrouped(start, end);
        long totalDeclines = declineData.stream().mapToLong(d -> (Long) d[1]).sum();
        List<DashboardResponse.DeclineReasonStats> declineStats = declineData.stream()
            .map(d -> {
                DeclineReason reason = (DeclineReason) d[0];
                long count = (Long) d[1];
                return DashboardResponse.DeclineReasonStats.builder()
                    .reason(reason.name())
                    .description(reason.getDescription())
                    .count(count)
                    .percentage(totalDeclines > 0 ? (double) count / totalDeclines * 100 : 0)
                    .isRetryable(reason.isRetryable())
                    .build();
            }).collect(Collectors.toList());

        return DashboardResponse.builder()
            .totalTransactions(total).successfulTransactions(success).failedTransactions(failed)
            .overallSuccessRate(Math.round(successRate * 100.0) / 100.0)
            .totalVolume(volume != null ? volume : BigDecimal.ZERO)
            .currency("INR")
            .paymentMethodBreakdown(methodStats)
            .providerBreakdown(providerStats)
            .topDeclineReasons(declineStats)
            .build();
    }

    private double calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
}
