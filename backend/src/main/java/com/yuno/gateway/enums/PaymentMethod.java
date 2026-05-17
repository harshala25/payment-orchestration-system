package com.yuno.gateway.enums;

/**
 * Supported payment methods in the orchestration platform.
 * Each method maps to specific provider connectors via routing rules.
 */
public enum PaymentMethod {
    CARD,    // Credit/Debit card payments → routed to Provider A
    UPI,     // Unified Payment Interface → routed to Provider B
    WALLET,  // Digital wallet (future extension)
    NETBANKING // Net banking (future extension)
}
