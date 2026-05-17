package com.yuno.gateway.enums;

/**
 * Payment lifecycle status - follows a strict state machine:
 * 
 * INITIATED → PROCESSING → SUCCESS
 *                        → FAILED → RETRY → PROCESSING (retry loop)
 * SUCCESS → REFUND_INITIATED → REFUNDED
 * 
 * State transitions are enforced at the service layer with optimistic locking.
 */
public enum PaymentStatus {
    INITIATED,          // Payment created, not yet sent to provider
    PROCESSING,         // Sent to provider, awaiting response
    SUCCESS,            // Provider confirmed payment success
    FAILED,             // Provider declined or error occurred
    RETRY,              // Marked for retry via failover provider
    REFUND_INITIATED,   // Refund request sent to provider
    REFUNDED,           // Refund confirmed by provider
    CANCELLED           // Payment cancelled before processing
}
