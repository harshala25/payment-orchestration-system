package com.yuno.gateway.service;

import com.yuno.gateway.entity.Provider;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.ProviderCode;
import com.yuno.gateway.repository.ProviderRepository;
import com.yuno.gateway.repository.RoutingAttemptRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Approval Rate Optimization Service - Core Pillar #6
 * Tracks approval rates per provider, auto-disables underperformers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalRateService {

    private final ProviderRepository providerRepository;
    private final RoutingAttemptRepository routingAttemptRepository;
    private final ComplianceService complianceService;

    @Value("${app.approval-rate.threshold-percentage:60.0}")
    private double approvalThreshold;

    @Value("${app.approval-rate.evaluation-window-minutes:60}")
    private int evaluationWindowMinutes;

    @Value("${app.approval-rate.auto-disable-on-low-rate:true}")
    private boolean autoDisable;

    public void recordSuccess(ProviderCode code, PaymentMethod method) {
        updateProviderStats(code, true);
    }

    public void recordFailure(ProviderCode code, PaymentMethod method) {
        updateProviderStats(code, false);
    }

    private void updateProviderStats(ProviderCode code, boolean success) {
        Optional<Provider> opt = providerRepository.findByCode(code);
        if (opt.isEmpty()) return;
        Provider p = opt.get();
        p.setTotalTransactions(p.getTotalTransactions() + 1);
        if (success) p.setSuccessfulTransactions(p.getSuccessfulTransactions() + 1);
        double rate = (double) p.getSuccessfulTransactions() / p.getTotalTransactions() * 100;
        p.setCurrentApprovalRate(Math.round(rate * 100.0) / 100.0);
        providerRepository.save(p);
    }

    public double calculateApprovalRate(ProviderCode code, int windowMinutes) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(windowMinutes);
        long total = routingAttemptRepository.countByProviderAndDateRange(code, start, end);
        if (total == 0) return 100.0;
        long success = routingAttemptRepository.countSuccessByProviderAndDateRange(code, start, end);
        return (double) success / total * 100;
    }

    public Map<ProviderCode, Double> getAllApprovalRates() {
        Map<ProviderCode, Double> rates = new HashMap<>();
        for (ProviderCode code : ProviderCode.values()) {
            rates.put(code, calculateApprovalRate(code, evaluationWindowMinutes));
        }
        return rates;
    }

    @Scheduled(fixedRate = 300000)
    public void evaluateProviderHealth() {
        log.info("Running scheduled provider health evaluation...");
        for (ProviderCode code : ProviderCode.values()) {
            double rate = calculateApprovalRate(code, evaluationWindowMinutes);
            Optional<Provider> opt = providerRepository.findByCode(code);
            if (opt.isEmpty()) continue;
            Provider p = opt.get();
            p.setCurrentApprovalRate(rate);
            p.setLastHealthCheck(LocalDateTime.now());
            if (rate >= 80) p.setHealthStatus("HEALTHY");
            else if (rate >= approvalThreshold) p.setHealthStatus("DEGRADED");
            else {
                p.setHealthStatus("DOWN");
                if (autoDisable && p.isEnabled()) {
                    log.warn("AUTO-DISABLING {} due to low approval rate: {}%", code, rate);
                    p.setEnabled(false);
                    p.setLastDowntime(LocalDateTime.now());
                    complianceService.recordProviderChange(code.name(), "AUTO_DISABLE",
                        "enabled=true", "enabled=false (rate=" + rate + "%)");
                }
            }
            Double avgLat = routingAttemptRepository.avgLatencyByProviderAndDateRange(
                code, LocalDateTime.now().minusMinutes(evaluationWindowMinutes), LocalDateTime.now());
            if (avgLat != null) p.setAverageLatencyMs(avgLat);
            providerRepository.save(p);
        }
    }

    public ProviderHealthSummary getProviderHealth(ProviderCode code) {
        Provider p = providerRepository.findByCode(code).orElse(null);
        if (p == null) return null;
        return ProviderHealthSummary.builder()
            .providerCode(code).providerName(p.getName()).isEnabled(p.isEnabled())
            .healthStatus(p.getHealthStatus()).approvalRate(p.getCurrentApprovalRate())
            .totalTransactions(p.getTotalTransactions())
            .successfulTransactions(p.getSuccessfulTransactions())
            .averageLatencyMs(p.getAverageLatencyMs())
            .lastHealthCheck(p.getLastHealthCheck()).build();
    }

    @Builder @Getter
    public static class ProviderHealthSummary {
        private ProviderCode providerCode;
        private String providerName;
        private boolean isEnabled;
        private String healthStatus;
        private double approvalRate;
        private long totalTransactions;
        private long successfulTransactions;
        private double averageLatencyMs;
        private LocalDateTime lastHealthCheck;
    }
}
