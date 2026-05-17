package com.yuno.gateway.controller;

import com.yuno.gateway.dto.request.CreatePaymentRequest;
import com.yuno.gateway.dto.response.PaymentResponse;
import com.yuno.gateway.enums.PaymentStatus;
import com.yuno.gateway.service.PaymentOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing and management APIs")
public class PaymentController {

    private final PaymentOrchestrationService orchestrationService;

    @PostMapping
    @Operation(summary = "Create a new payment",
               description = "Initiates a payment through the orchestration pipeline with smart routing and failover")
    @ApiResponse(responseCode = "201", description = "Payment created and processed")
    @ApiResponse(responseCode = "200", description = "Idempotent response - payment already processed")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") 
            @Parameter(description = "Unique key to prevent duplicate processing") String idempotencyKey) {

        PaymentResponse response = orchestrationService.createPayment(request, idempotencyKey);
        
        HttpStatus status = (response.getStatus() == PaymentStatus.SUCCESS || 
                              response.getStatus() == PaymentStatus.FAILED) 
                             ? HttpStatus.CREATED : HttpStatus.OK;
        
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Fetch payment by ID", description = "Retrieve details of a specific payment")
    @ApiResponse(responseCode = "200", description = "Payment found")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(orchestrationService.getPayment(paymentId));
    }

    @GetMapping
    @Operation(summary = "List payments", description = "Paginated list of payments with optional filters")
    public ResponseEntity<Page<PaymentResponse>> listPayments(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orchestrationService.listPayments(merchantId, status, pageable));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a payment", description = "Initiate a refund for a successful payment")
    @ApiResponse(responseCode = "200", description = "Refund processed")
    @ApiResponse(responseCode = "400", description = "Payment not eligible for refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(orchestrationService.refundPayment(paymentId));
    }
}
