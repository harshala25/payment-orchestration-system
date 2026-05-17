package com.yuno.gateway.dto.request;

import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.ProviderCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating or updating a routing rule")
public class RoutingRuleRequest {

    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment method this rule applies to", example = "CARD")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Provider code is required")
    @Schema(description = "Target provider for routing", example = "PROVIDER_A")
    private ProviderCode providerCode;

    @Min(value = 1, message = "Priority must be >= 1")
    @Max(value = 100, message = "Priority must be <= 100")
    @Schema(description = "Rule priority (1 = highest)", example = "1")
    private int priority;

    @Min(value = 0, message = "Weight must be >= 0")
    @Max(value = 100, message = "Weight must be <= 100")
    @Schema(description = "Traffic weight percentage (0-100)", example = "100")
    private int weight;

    @Schema(description = "Rule description", example = "Route all card payments to Provider A")
    private String description;

    @Schema(description = "Whether the rule is active", example = "true")
    private boolean isActive;
}
