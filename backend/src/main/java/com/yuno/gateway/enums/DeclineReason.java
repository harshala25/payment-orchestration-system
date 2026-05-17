package com.yuno.gateway.enums;

/**
 * Categorized decline reasons from payment providers.
 * 
 * Soft declines (retryable): TIMEOUT, BANK_UNAVAILABLE, NETWORK_ERROR
 * Hard declines (non-retryable): INSUFFICIENT_FUNDS, CARD_EXPIRED, DO_NOT_HONOR, 
 *                                 VPA_NOT_FOUND, FRAUD_SUSPECTED
 */
public enum DeclineReason {
    // Hard declines - do NOT retry
    INSUFFICIENT_FUNDS("Insufficient funds in account"),
    CARD_EXPIRED("Card has expired"),
    DO_NOT_HONOR("Issuer declined - do not honor"),
    VPA_NOT_FOUND("UPI VPA not registered"),
    FRAUD_SUSPECTED("Transaction flagged as potentially fraudulent"),
    INVALID_CARD_NUMBER("Invalid card number"),
    
    // Soft declines - eligible for retry
    TIMEOUT("Provider did not respond in time"),
    BANK_UNAVAILABLE("Issuing bank temporarily unavailable"),
    NETWORK_ERROR("Network connectivity issue with provider"),
    RATE_LIMITED("Provider rate limit exceeded"),
    
    // Unknown
    UNKNOWN("Unclassified decline reason");

    private final String description;

    DeclineReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines if this decline reason is retryable (soft decline).
     * Only soft declines should trigger automatic retry/failover.
     */
    public boolean isRetryable() {
        return this == TIMEOUT || this == BANK_UNAVAILABLE || 
               this == NETWORK_ERROR || this == RATE_LIMITED;
    }
}
