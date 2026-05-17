package com.yuno.gateway.repository;

import com.yuno.gateway.entity.Provider;
import com.yuno.gateway.enums.ProviderCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    Optional<Provider> findByCode(ProviderCode code);

    List<Provider> findByIsEnabledTrue();

    List<Provider> findByHealthStatus(String healthStatus);
}
