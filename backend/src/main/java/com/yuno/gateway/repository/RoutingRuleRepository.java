package com.yuno.gateway.repository;

import com.yuno.gateway.entity.RoutingRule;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.ProviderCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {

    List<RoutingRule> findByPaymentMethodAndIsActiveTrueOrderByPriorityAsc(PaymentMethod method);

    List<RoutingRule> findByIsActiveTrueOrderByPaymentMethodAscPriorityAsc();

    Optional<RoutingRule> findByPaymentMethodAndProviderCode(PaymentMethod method, ProviderCode code);

    List<RoutingRule> findByProviderCode(ProviderCode code);
}
