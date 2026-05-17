package com.yuno.gateway.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dashboard summary response aggregating key metrics for the UI.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DashboardResponse {

    // Overview
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private double overallSuccessRate;
    private BigDecimal totalVolume;
    private String currency;

    // By payment method
    private List<PaymentMethodStats> paymentMethodBreakdown;

    // By provider
    private List<ProviderStats> providerBreakdown;

    // Top decline reasons
    private List<DeclineReasonStats> topDeclineReasons;

    // Time series (last 24h, hourly)
    private List<TimeSeriesPoint> volumeTrend;
    private List<TimeSeriesPoint> successRateTrend;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentMethodStats {
        private String paymentMethod;
        private long totalCount;
        private long successCount;
        private double approvalRate;
        private BigDecimal totalVolume;
        private double avgLatencyMs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProviderStats {
        private String providerCode;
        private String providerName;
        private String healthStatus;
        private boolean isEnabled;
        private long totalCount;
        private long successCount;
        private double approvalRate;
        private double avgLatencyMs;
        private double p95LatencyMs;
        private double p99LatencyMs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DeclineReasonStats {
        private String reason;
        private String description;
        private long count;
        private double percentage;
        private boolean isRetryable;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TimeSeriesPoint {
        private String timestamp;
        private double value;
        private long count;
    }
}
