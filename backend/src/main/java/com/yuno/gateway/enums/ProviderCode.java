package com.yuno.gateway.enums;

/**
 * Registered payment provider identifiers.
 * Each provider has a dedicated connector implementing the ProviderConnector interface.
 */
public enum ProviderCode {
    PROVIDER_A,  // CardPay Global - Primary card payment processor
    PROVIDER_B   // UPI Direct Connect - Primary UPI payment processor
}
