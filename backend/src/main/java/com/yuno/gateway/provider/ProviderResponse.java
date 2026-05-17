package com.yuno.gateway.provider;

import com.yuno.gateway.enums.DeclineReason;
import com.yuno.gateway.enums.ProviderCode;
import lombok.*;

/**
 * Standardized response from payment provider connectors.
 * All provider-specific responses are mapped to this common format.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProviderResponse {

    private boolean success;
    private ProviderCode providerCode;
    private String providerTransactionId;
    private DeclineReason declineReason;
    private String rawResponse;
    private long latencyMs;
    private String errorMessage;

    public static ProviderResponse success(ProviderCode code, String txnId, long latency) {
        return ProviderResponse.builder()
                .success(true)
                .providerCode(code)
                .providerTransactionId(txnId)
                .latencyMs(latency)
                .rawResponse("{\"status\": \"approved\"}")
                .build();
    }

    public static ProviderResponse failure(ProviderCode code, DeclineReason reason, long latency, String message) {
        return ProviderResponse.builder()
                .success(false)
                .providerCode(code)
                .declineReason(reason)
                .latencyMs(latency)
                .errorMessage(message)
                .rawResponse("{\"status\": \"declined\", \"reason\": \"" + reason.name() + "\"}")
                .build();
    }
}
