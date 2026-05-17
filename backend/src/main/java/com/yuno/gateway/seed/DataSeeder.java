package com.yuno.gateway.seed;

import com.yuno.gateway.entity.Provider;
import com.yuno.gateway.entity.RoutingRule;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.ProviderCode;
import com.yuno.gateway.repository.ProviderRepository;
import com.yuno.gateway.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds initial data on application startup.
 * Creates default providers and routing rules if they don't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProviderRepository providerRepository;
    private final RoutingRuleRepository routingRuleRepository;

    @Override
    public void run(String... args) {
        seedProviders();
        seedRoutingRules();
        log.info("=== Data seeding complete ===");
    }

    private void seedProviders() {
        if (providerRepository.findByCode(ProviderCode.PROVIDER_A).isEmpty()) {
            providerRepository.save(Provider.builder()
                .name("CardPay Global")
                .code(ProviderCode.PROVIDER_A)
                .isEnabled(true)
                .healthStatus("HEALTHY")
                .supportedMethods("CARD,WALLET")
                .baseUrl("https://simulated-provider-a.yuno.test/api")
                .timeoutMs(3000)
                .build());
            log.info("Seeded Provider A: CardPay Global");
        }

        if (providerRepository.findByCode(ProviderCode.PROVIDER_B).isEmpty()) {
            providerRepository.save(Provider.builder()
                .name("UPI Direct Connect")
                .code(ProviderCode.PROVIDER_B)
                .isEnabled(true)
                .healthStatus("HEALTHY")
                .supportedMethods("UPI,NETBANKING")
                .baseUrl("https://simulated-provider-b.yuno.test/api")
                .timeoutMs(2000)
                .build());
            log.info("Seeded Provider B: UPI Direct Connect");
        }
    }

    private void seedRoutingRules() {
        if (routingRuleRepository.findByPaymentMethodAndProviderCode(
                PaymentMethod.CARD, ProviderCode.PROVIDER_A).isEmpty()) {
            routingRuleRepository.save(RoutingRule.builder()
                .paymentMethod(PaymentMethod.CARD)
                .providerCode(ProviderCode.PROVIDER_A)
                .priority(1)
                .weight(100)
                .isActive(true)
                .description("Route all CARD payments to Provider A (CardPay Global)")
                .build());
            log.info("Seeded routing rule: CARD → PROVIDER_A (priority 1)");
        }

        // Failover: CARD can also go to PROVIDER_B at lower priority
        if (routingRuleRepository.findByPaymentMethodAndProviderCode(
                PaymentMethod.CARD, ProviderCode.PROVIDER_B).isEmpty()) {
            routingRuleRepository.save(RoutingRule.builder()
                .paymentMethod(PaymentMethod.CARD)
                .providerCode(ProviderCode.PROVIDER_B)
                .priority(2)
                .weight(100)
                .isActive(true)
                .description("Failover: CARD payments to Provider B if Provider A is unavailable")
                .build());
            log.info("Seeded routing rule: CARD → PROVIDER_B (priority 2, failover)");
        }

        if (routingRuleRepository.findByPaymentMethodAndProviderCode(
                PaymentMethod.UPI, ProviderCode.PROVIDER_B).isEmpty()) {
            routingRuleRepository.save(RoutingRule.builder()
                .paymentMethod(PaymentMethod.UPI)
                .providerCode(ProviderCode.PROVIDER_B)
                .priority(1)
                .weight(100)
                .isActive(true)
                .description("Route all UPI payments to Provider B (UPI Direct Connect)")
                .build());
            log.info("Seeded routing rule: UPI → PROVIDER_B (priority 1)");
        }

        // Failover: UPI can also go to PROVIDER_A at lower priority
        if (routingRuleRepository.findByPaymentMethodAndProviderCode(
                PaymentMethod.UPI, ProviderCode.PROVIDER_A).isEmpty()) {
            routingRuleRepository.save(RoutingRule.builder()
                .paymentMethod(PaymentMethod.UPI)
                .providerCode(ProviderCode.PROVIDER_A)
                .priority(2)
                .weight(100)
                .isActive(true)
                .description("Failover: UPI payments to Provider A if Provider B is unavailable")
                .build());
            log.info("Seeded routing rule: UPI → PROVIDER_A (priority 2, failover)");
        }
    }
}
