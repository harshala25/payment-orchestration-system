package com.yuno.gateway.service;

import com.yuno.gateway.dto.response.PaymentResponse;
import com.yuno.gateway.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("Should return null for new idempotency key (cache miss)")
    void shouldReturnNullForNewKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        PaymentResponse result = idempotencyService.checkDuplicate("new-key-001");
        assertNull(result);
    }

    @Test
    @DisplayName("Should acquire lock for new idempotency key")
    void shouldAcquireLockForNewKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        boolean acquired = idempotencyService.acquireLock("lock-key-001");
        assertTrue(acquired);
    }

    @Test
    @DisplayName("Should NOT acquire lock for concurrent duplicate request")
    void shouldNotAcquireLockForDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);

        boolean acquired = idempotencyService.acquireLock("lock-key-002");
        assertFalse(acquired);
    }

    @Test
    @DisplayName("Should store response in Redis with TTL")
    void shouldStoreResponseInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        PaymentResponse response = PaymentResponse.builder()
            .id(UUID.randomUUID())
            .status(PaymentStatus.SUCCESS)
            .build();

        assertDoesNotThrow(() -> idempotencyService.storeResponse("store-key-001", response));
        verify(valueOps).set(eq("idempotency:store-key-001"), eq(response), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should handle Redis failure gracefully (fail open)")
    void shouldHandleRedisFailureGracefully() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        // Should not throw - fail open
        PaymentResponse result = idempotencyService.checkDuplicate("fail-key");
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle lock failure gracefully (fail open)")
    void shouldHandleLockFailureGracefully() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        // Should return true (fail open) - let PostgreSQL unique constraint catch duplicates
        boolean acquired = idempotencyService.acquireLock("fail-lock");
        assertTrue(acquired);
    }
}
