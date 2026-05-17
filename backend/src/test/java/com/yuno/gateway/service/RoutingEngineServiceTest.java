package com.yuno.gateway.service;

import com.yuno.gateway.entity.Provider;
import com.yuno.gateway.entity.RoutingRule;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.ProviderCode;
import com.yuno.gateway.exception.RoutingException;
import com.yuno.gateway.provider.ProviderConnectorFactory;
import com.yuno.gateway.repository.ProviderRepository;
import com.yuno.gateway.repository.RoutingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingEngineServiceTest {

    @Mock private RoutingRuleRepository routingRuleRepository;
    @Mock private ProviderRepository providerRepository;
    @Mock private ProviderConnectorFactory connectorFactory;

    @InjectMocks
    private RoutingEngineService routingEngineService;

    private Provider providerA;
    private Provider providerB;

    @BeforeEach
    void setUp() {
        providerA = Provider.builder()
            .code(ProviderCode.PROVIDER_A).name("CardPay").isEnabled(true)
            .healthStatus("HEALTHY").build();
        providerB = Provider.builder()
            .code(ProviderCode.PROVIDER_B).name("UPI Direct").isEnabled(true)
            .healthStatus("HEALTHY").build();
    }

    @Test
    @DisplayName("Should resolve CARD → PROVIDER_A as primary")
    void shouldRouteCardToProviderA() {
        List<RoutingRule> rules = List.of(
            RoutingRule.builder().paymentMethod(PaymentMethod.CARD)
                .providerCode(ProviderCode.PROVIDER_A).priority(1).weight(100).isActive(true).build(),
            RoutingRule.builder().paymentMethod(PaymentMethod.CARD)
                .providerCode(ProviderCode.PROVIDER_B).priority(2).weight(100).isActive(true).build()
        );

        when(routingRuleRepository.findByPaymentMethodAndIsActiveTrueOrderByPriorityAsc(PaymentMethod.CARD))
            .thenReturn(rules);
        when(providerRepository.findByCode(ProviderCode.PROVIDER_A)).thenReturn(Optional.of(providerA));
        when(providerRepository.findByCode(ProviderCode.PROVIDER_B)).thenReturn(Optional.of(providerB));
        when(connectorFactory.isProviderAvailable(any())).thenReturn(true);

        List<ProviderCode> chain = routingEngineService.resolveProviderChain(PaymentMethod.CARD);

        assertEquals(2, chain.size());
        assertEquals(ProviderCode.PROVIDER_A, chain.get(0)); // Primary
        assertEquals(ProviderCode.PROVIDER_B, chain.get(1)); // Failover
    }

    @Test
    @DisplayName("Should resolve UPI → PROVIDER_B as primary")
    void shouldRouteUpiToProviderB() {
        List<RoutingRule> rules = List.of(
            RoutingRule.builder().paymentMethod(PaymentMethod.UPI)
                .providerCode(ProviderCode.PROVIDER_B).priority(1).weight(100).isActive(true).build()
        );

        when(routingRuleRepository.findByPaymentMethodAndIsActiveTrueOrderByPriorityAsc(PaymentMethod.UPI))
            .thenReturn(rules);
        when(providerRepository.findByCode(ProviderCode.PROVIDER_B)).thenReturn(Optional.of(providerB));
        when(connectorFactory.isProviderAvailable(ProviderCode.PROVIDER_B)).thenReturn(true);

        List<ProviderCode> chain = routingEngineService.resolveProviderChain(PaymentMethod.UPI);

        assertEquals(1, chain.size());
        assertEquals(ProviderCode.PROVIDER_B, chain.get(0));
    }

    @Test
    @DisplayName("Should skip disabled providers in chain")
    void shouldSkipDisabledProviders() {
        providerA.setEnabled(false);

        List<RoutingRule> rules = List.of(
            RoutingRule.builder().paymentMethod(PaymentMethod.CARD)
                .providerCode(ProviderCode.PROVIDER_A).priority(1).weight(100).isActive(true).build(),
            RoutingRule.builder().paymentMethod(PaymentMethod.CARD)
                .providerCode(ProviderCode.PROVIDER_B).priority(2).weight(100).isActive(true).build()
        );

        when(routingRuleRepository.findByPaymentMethodAndIsActiveTrueOrderByPriorityAsc(PaymentMethod.CARD))
            .thenReturn(rules);
        when(providerRepository.findByCode(ProviderCode.PROVIDER_A)).thenReturn(Optional.of(providerA));
        when(providerRepository.findByCode(ProviderCode.PROVIDER_B)).thenReturn(Optional.of(providerB));
        when(connectorFactory.isProviderAvailable(ProviderCode.PROVIDER_B)).thenReturn(true);

        List<ProviderCode> chain = routingEngineService.resolveProviderChain(PaymentMethod.CARD);

        assertEquals(1, chain.size());
        assertEquals(ProviderCode.PROVIDER_B, chain.get(0)); // Failover becomes primary
    }

    @Test
    @DisplayName("Should throw RoutingException when no rules configured")
    void shouldThrowWhenNoRulesConfigured() {
        when(routingRuleRepository.findByPaymentMethodAndIsActiveTrueOrderByPriorityAsc(PaymentMethod.WALLET))
            .thenReturn(Collections.emptyList());

        assertThrows(RoutingException.class, 
            () -> routingEngineService.resolveProviderChain(PaymentMethod.WALLET));
    }

    @Test
    @DisplayName("Should throw when all providers are down")
    void shouldThrowWhenAllProvidersDown() {
        providerA.setHealthStatus("DOWN");
        providerA.setEnabled(false);

        List<RoutingRule> rules = List.of(
            RoutingRule.builder().paymentMethod(PaymentMethod.CARD)
                .providerCode(ProviderCode.PROVIDER_A).priority(1).weight(100).isActive(true).build()
        );

        when(routingRuleRepository.findByPaymentMethodAndIsActiveTrueOrderByPriorityAsc(PaymentMethod.CARD))
            .thenReturn(rules);
        when(providerRepository.findByCode(ProviderCode.PROVIDER_A)).thenReturn(Optional.of(providerA));

        assertThrows(RoutingException.class, 
            () -> routingEngineService.resolveProviderChain(PaymentMethod.CARD));
    }
}
