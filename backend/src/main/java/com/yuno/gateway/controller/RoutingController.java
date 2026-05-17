package com.yuno.gateway.controller;

import com.yuno.gateway.dto.request.RoutingRuleRequest;
import com.yuno.gateway.entity.Provider;
import com.yuno.gateway.entity.RoutingRule;
import com.yuno.gateway.enums.ProviderCode;
import com.yuno.gateway.provider.ProviderAConnector;
import com.yuno.gateway.provider.ProviderBConnector;
import com.yuno.gateway.repository.ProviderRepository;
import com.yuno.gateway.service.ApprovalRateService;
import com.yuno.gateway.service.ComplianceService;
import com.yuno.gateway.service.RoutingEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
@Tag(name = "Routing", description = "Dynamic routing engine management")
public class RoutingController {

    private final RoutingEngineService routingEngine;
    private final ProviderRepository providerRepository;
    private final ApprovalRateService approvalRateService;
    private final ComplianceService complianceService;
    private final ProviderAConnector providerAConnector;
    private final ProviderBConnector providerBConnector;

    @GetMapping("/rules")
    @Operation(summary = "List all active routing rules")
    public ResponseEntity<List<RoutingRule>> getRules() {
        return ResponseEntity.ok(routingEngine.getAllRules());
    }

    @PostMapping("/rules")
    @Operation(summary = "Create or update a routing rule")
    public ResponseEntity<RoutingRule> createRule(@Valid @RequestBody RoutingRuleRequest request) {
        RoutingRule rule = RoutingRule.builder()
            .paymentMethod(request.getPaymentMethod())
            .providerCode(request.getProviderCode())
            .priority(request.getPriority())
            .weight(request.getWeight())
            .description(request.getDescription())
            .isActive(request.isActive())
            .build();
        return ResponseEntity.ok(routingEngine.saveRule(rule));
    }

    @GetMapping("/providers")
    @Operation(summary = "List all providers with health status")
    public ResponseEntity<List<Provider>> getProviders() {
        return ResponseEntity.ok(providerRepository.findAll());
    }

    @GetMapping("/providers/{code}/health")
    @Operation(summary = "Get detailed health summary for a provider")
    public ResponseEntity<ApprovalRateService.ProviderHealthSummary> getProviderHealth(
            @PathVariable ProviderCode code) {
        ApprovalRateService.ProviderHealthSummary summary = approvalRateService.getProviderHealth(code);
        if (summary == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/providers/{code}/toggle")
    @Operation(summary = "Enable or disable a provider")
    public ResponseEntity<Provider> toggleProvider(
            @PathVariable ProviderCode code,
            @RequestParam boolean enabled) {
        Provider provider = providerRepository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + code));

        String oldState = "enabled=" + provider.isEnabled();
        provider.setEnabled(enabled);
        provider = providerRepository.save(provider);

        // Also toggle the connector health for simulation
        if (code == ProviderCode.PROVIDER_A) providerAConnector.setHealthy(enabled);
        if (code == ProviderCode.PROVIDER_B) providerBConnector.setHealthy(enabled);

        complianceService.recordProviderChange(code.name(), "TOGGLE",
            oldState, "enabled=" + enabled);

        return ResponseEntity.ok(provider);
    }

    @GetMapping("/approval-rates")
    @Operation(summary = "Get current approval rates for all providers")
    public ResponseEntity<Map<String, Double>> getApprovalRates() {
        Map<ProviderCode, Double> rates = approvalRateService.getAllApprovalRates();
        Map<String, Double> result = rates.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        return ResponseEntity.ok(result);
    }
}
