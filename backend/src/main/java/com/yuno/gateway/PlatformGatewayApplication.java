package com.yuno.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Orchestration System Application
 * 
 * A production-grade Payment Orchestration System that enables merchants
 * to process payments across multiple providers via a single unified API.
 * 
 * Core Pillars:
 * 1. API-First Architecture (OpenAPI 3.0)
 * 2. Dynamic Routing Engine (Rule-based + Weighted)
 * 3. Transaction Management (Idempotency + State Machine)
 * 4. Compliance Framework (PCI-DSS, Audit Trail)
 * 5. Analytics & Reporting Module
 * 6. Approval Rate Optimization
 * 
 * @author Yuno Assessment - Backend Core Java
 */
@SpringBootApplication
@EnableScheduling
public class PlatformGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformGatewayApplication.class, args);
    }
}
