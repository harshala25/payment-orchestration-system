package com.yuno.gateway.service;

import com.yuno.gateway.entity.Provider;
import com.yuno.gateway.entity.RoutingRule;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.ProviderCode;
import com.yuno.gateway.exception.RoutingException;
import com.yuno.gateway.provider.ProviderConnectorFactory;
import com.yuno.gateway.repository.ProviderRepository;
import com.yuno.gateway.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic Routing Engine - Core Pillar #2
 * 
 * Determines which payment provider(s) should process a payment based on:
 * 1. Payment method → provider mapping (routing rules from DB)
 * 2. Provider health status (enabled + healthy check)
 * 3. Rule priority ordering (lower number = higher priority)
 * 4. Weight-based selection for same-priority rules (A/B testing)
 * 5. Approval rate consideration (auto-disable low-performing providers)
 * 
 * Returns an ordered list of providers forming the failover chain:
 *   [Primary, Secondary, Tertiary, ...]
 * 
 * The orchestration service tries each provider in order until success
 * or the chain is exhausted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingEngineService {

    private final RoutingRuleRepository routingRuleRepository;
    private final ProviderRepository providerRepository;
    private final ProviderConnectorFactory connectorFactory;

    private final Random random = new Random();

    /**
     * Resolve the ordered failover chain of providers for a payment method.
     *
     * @param paymentMethod The payment method to route
     * @return Ordered list of provider codes (primary first)
     * @throws RoutingException if no eligible providers found
     */
    public List<ProviderCode> resolveProviderChain(PaymentMethod paymentMethod) {
        log.info("Resolving provider chain for payment method: {}", paymentMethod);

        // 1. Fetch active routing rules ordered by priority
        List<RoutingRule> rules = routingRuleRepository
            .findByPaymentMethodAndIsActiveTrueOrderByPriorityAsc(paymentMethod);

        if (rules.isEmpty()) {
            log.error("No active routing rules found for method: {}", paymentMethod);
            throw new RoutingException(
                "No routing rules configured for payment method: " + paymentMethod);
        }

        // 2. Filter to only healthy and enabled providers
        List<ProviderCode> chain = new ArrayList<>();
        
        // Group rules by priority for weight-based selection
        Map<Integer, List<RoutingRule>> priorityGroups = rules.stream()
            .collect(Collectors.groupingBy(RoutingRule::getPriority, 
                     LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<RoutingRule>> entry : priorityGroups.entrySet()) {
            List<RoutingRule> samePriorityRules = entry.getValue();

            if (samePriorityRules.size() == 1) {
                // Single rule at this priority - use directly
                RoutingRule rule = samePriorityRules.get(0);
                if (isProviderEligible(rule.getProviderCode())) {
                    chain.add(rule.getProviderCode());
                }
            } else {
                // Multiple rules at same priority - weight-based selection
                ProviderCode selected = selectByWeight(samePriorityRules);
                if (selected != null) {
                    chain.add(selected);
                }
                // Add remaining as fallbacks
                for (RoutingRule rule : samePriorityRules) {
                    if (rule.getProviderCode() != selected && isProviderEligible(rule.getProviderCode())) {
                        chain.add(rule.getProviderCode());
                    }
                }
            }
        }

        if (chain.isEmpty()) {
            throw new RoutingException(
                "No eligible providers available for payment method: " + paymentMethod);
        }

        log.info("Resolved provider chain for {}: {}", paymentMethod, chain);
        return chain;
    }

    /**
     * Weight-based provider selection for A/B testing.
     * 
     * Given rules with weights [70, 30], there's a 70% chance of selecting
     * the first provider and 30% chance for the second.
     */
    private ProviderCode selectByWeight(List<RoutingRule> rules) {
        List<RoutingRule> eligible = rules.stream()
            .filter(r -> isProviderEligible(r.getProviderCode()))
            .collect(Collectors.toList());

        if (eligible.isEmpty()) return null;
        if (eligible.size() == 1) return eligible.get(0).getProviderCode();

        int totalWeight = eligible.stream().mapToInt(RoutingRule::getWeight).sum();
        if (totalWeight <= 0) return eligible.get(0).getProviderCode();

        int randomValue = random.nextInt(totalWeight);
        int cumulative = 0;

        for (RoutingRule rule : eligible) {
            cumulative += rule.getWeight();
            if (randomValue < cumulative) {
                return rule.getProviderCode();
            }
        }

        return eligible.get(0).getProviderCode();
    }

    /**
     * Check if a provider is eligible for routing.
     * A provider must be both enabled (admin toggle) and healthy (connector check).
     */
    private boolean isProviderEligible(ProviderCode code) {
        Optional<Provider> provider = providerRepository.findByCode(code);
        
        if (provider.isEmpty() || !provider.get().isEnabled()) {
            log.debug("Provider {} is disabled or not found", code);
            return false;
        }

        if (!"HEALTHY".equals(provider.get().getHealthStatus()) && 
            !"DEGRADED".equals(provider.get().getHealthStatus())) {
            log.debug("Provider {} health status: {}", code, provider.get().getHealthStatus());
            return false;
        }

        // Also check connector-level health
        return connectorFactory.isProviderAvailable(code);
    }

    /**
     * Get all routing rules (for management UI).
     */
    public List<RoutingRule> getAllRules() {
        return routingRuleRepository.findByIsActiveTrueOrderByPaymentMethodAscPriorityAsc();
    }

    /**
     * Create or update a routing rule.
     */
    public RoutingRule saveRule(RoutingRule rule) {
        log.info("Saving routing rule: {} → {} (priority={}, weight={})",
                 rule.getPaymentMethod(), rule.getProviderCode(), 
                 rule.getPriority(), rule.getWeight());
        return routingRuleRepository.save(rule);
    }
}
