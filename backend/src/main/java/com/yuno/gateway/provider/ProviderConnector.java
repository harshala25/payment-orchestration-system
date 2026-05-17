package com.yuno.gateway.provider;

import com.yuno.gateway.dto.request.CreatePaymentRequest;
import com.yuno.gateway.enums.ProviderCode;

/**
 * Common interface for all payment provider connectors.
 * 
 * Each provider implements this contract, abstracting away
 * the specifics of individual PSP APIs. This enables:
 * - Easy addition of new providers
 * - Consistent error handling across providers
 * - Testability through mock implementations
 */
public interface ProviderConnector {

    /**
     * Process a payment through this provider.
     * 
     * @param request The payment creation request
     * @return Standardized provider response
     */
    ProviderResponse processPayment(CreatePaymentRequest request);

    /**
     * Check the status of an existing payment.
     * 
     * @param providerTransactionId The provider's transaction reference
     * @return Standardized provider response with current status
     */
    ProviderResponse checkStatus(String providerTransactionId);

    /**
     * Initiate a refund for a completed payment.
     * 
     * @param providerTransactionId The provider's transaction reference
     * @param amount Refund amount
     * @return Standardized provider response
     */
    ProviderResponse refund(String providerTransactionId, java.math.BigDecimal amount);

    /**
     * @return The provider code this connector handles
     */
    ProviderCode getProviderCode();

    /**
     * Health check for the provider.
     * @return true if the provider is reachable and operational
     */
    boolean isHealthy();
}
