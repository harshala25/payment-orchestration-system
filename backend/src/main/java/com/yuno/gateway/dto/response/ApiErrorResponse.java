package com.yuno.gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized API error response envelope.
 * All errors follow this consistent format for easy client parsing.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Schema(description = "Standard error response")
public class ApiErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error code for programmatic handling", example = "VALIDATION_ERROR")
    private String error;

    @Schema(description = "Human-readable error message", example = "Invalid payment method")
    private String message;

    @Schema(description = "Request trace ID for debugging")
    private String traceId;

    @Schema(description = "Detailed field-level errors (for validation)")
    private List<FieldError> fieldErrors;

    @Schema(description = "Error timestamp")
    private LocalDateTime timestamp;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
