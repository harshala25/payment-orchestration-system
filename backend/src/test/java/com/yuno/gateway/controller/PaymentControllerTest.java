package com.yuno.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuno.gateway.dto.request.CreatePaymentRequest;
import com.yuno.gateway.dto.response.PaymentResponse;
import com.yuno.gateway.enums.PaymentMethod;
import com.yuno.gateway.enums.PaymentStatus;
import com.yuno.gateway.enums.ProviderCode;
import com.yuno.gateway.service.PaymentOrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentOrchestrationService orchestrationService;

    private static final String API_KEY = "test-api-key";
    private static final String IDEMPOTENCY_KEY = "idem-key-001";

    @Test
    @DisplayName("POST /api/v1/payments - Should create payment successfully")
    void shouldCreatePaymentSuccessfully() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId("merchant_001")
            .amount(new BigDecimal("1500.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .cardNumber("4242424242424242")
            .cardExpiryMonth("12")
            .cardExpiryYear("2028")
            .cardCvv("123")
            .description("Test payment")
            .build();

        PaymentResponse response = PaymentResponse.builder()
            .id(UUID.randomUUID())
            .merchantId("merchant_001")
            .amount(new BigDecimal("1500.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .status(PaymentStatus.SUCCESS)
            .providerUsed(ProviderCode.PROVIDER_A)
            .maskedCardNumber("****-****-****-4242")
            .cardBrand("VISA")
            .attemptCount(1)
            .routingAttempts(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .build();

        when(orchestrationService.createPayment(any(), eq(IDEMPOTENCY_KEY))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                .header("X-API-Key", API_KEY)
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.maskedCardNumber").value("****-****-****-4242"))
            .andExpect(jsonPath("$.providerUsed").value("PROVIDER_A"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - Should reject missing idempotency key")
    void shouldRejectMissingIdempotencyKey() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId("merchant_001")
            .amount(new BigDecimal("1500.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .build();

        mockMvc.perform(post("/api/v1/payments")
                .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/payments - Should reject invalid amount")
    void shouldRejectInvalidAmount() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId("merchant_001")
            .amount(new BigDecimal("-10.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .build();

        mockMvc.perform(post("/api/v1/payments")
                .header("X-API-Key", API_KEY)
                .header("Idempotency-Key", "key-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/payments - Should reject missing API key")
    void shouldRejectMissingApiKey() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .merchantId("merchant_001")
            .amount(new BigDecimal("1500.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .build();

        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", "key-003")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - Should fetch payment by ID")
    void shouldFetchPaymentById() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = PaymentResponse.builder()
            .id(paymentId)
            .merchantId("merchant_001")
            .amount(new BigDecimal("500.00"))
            .status(PaymentStatus.SUCCESS)
            .build();

        when(orchestrationService.getPayment(paymentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(paymentId.toString()))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/payments - Should list payments with pagination")
    void shouldListPaymentsWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                .header("X-API-Key", API_KEY)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk());
    }
}
