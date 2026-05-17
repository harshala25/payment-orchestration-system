package com.yuno.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for API-first architecture.
 * 
 * Provides comprehensive API documentation accessible at /swagger-ui.html
 * with API key security scheme configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Payment Orchestration System API")
                .description("""
                    A production-grade payment orchestration system that enables merchants 
                    to process payments across multiple providers via a single unified API.
                    
                    ## Core Features
                    - **Smart Routing**: Dynamic provider selection based on payment method and rules
                    - **Retry & Failover**: Automatic retry with exponential backoff and provider failover
                    - **Idempotency**: Guaranteed exactly-once payment processing
                    - **Compliance**: PCI-DSS aware with comprehensive audit trails
                    - **Analytics**: Real-time approval rates, provider performance, and trend analysis
                    
                    ## Authentication
                    All API endpoints require an `X-API-Key` header with a valid API key.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Payment Orchestration System Team")
                    .email("engineering@yuno.test"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development")))
            .addSecurityItem(new SecurityRequirement().addList("API-Key"))
            .components(new Components()
                .addSecuritySchemes("API-Key", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API key for merchant authentication")));
    }
}
