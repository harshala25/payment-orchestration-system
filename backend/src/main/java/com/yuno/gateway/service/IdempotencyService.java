package com.yuno.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuno.gateway.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Idempotency service using a two-layer approach:
 * 
 * Layer 1 (Redis): Fast in-memory check with TTL expiry.
 * Layer 2 (PostgreSQL): Unique constraint on idempotency_key column.
 * 
 * When Redis is unavailable (dev mode), all operations fall through
 * to the PostgreSQL unique constraint as the safety net.
 */
@Service
@Slf4j
public class IdempotencyService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final String LOCK_PREFIX = "idempotency_lock:";

    @Value("${app.idempotency.ttl-hours:24}")
    private int ttlHours;

    private boolean isRedisAvailable() {
        if (redisTemplate == null) return false;
        try {
            redisTemplate.hasKey("health-check");
            return true;
        } catch (Exception e) {
            log.debug("Redis not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if a request with this idempotency key has already been processed.
     */
    public PaymentResponse checkDuplicate(String idempotencyKey) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable — skipping idempotency cache check for key: {}", idempotencyKey);
            return null;
        }

        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                log.info("Idempotency HIT for key: {}", idempotencyKey);
                if (cached instanceof PaymentResponse) {
                    return (PaymentResponse) cached;
                }
                String json = objectMapper.writeValueAsString(cached);
                return objectMapper.readValue(json, PaymentResponse.class);
            }
            
            log.debug("Idempotency MISS for key: {}", idempotencyKey);
            return null;
        } catch (Exception e) {
            log.warn("Redis idempotency check failed, proceeding without cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Attempt to acquire a processing lock for this idempotency key.
     */
    public boolean acquireLock(String idempotencyKey) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable — skipping lock for key: {}", idempotencyKey);
            return true; // Fail open — let DB constraint catch duplicates
        }

        try {
            String lockKey = LOCK_PREFIX + idempotencyKey;
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "PROCESSING", 60, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Lock acquired for idempotency key: {}", idempotencyKey);
                return true;
            }
            
            log.info("Lock NOT acquired (concurrent request) for key: {}", idempotencyKey);
            return false;
        } catch (Exception e) {
            log.warn("Redis lock acquisition failed, proceeding: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Store the payment response for future idempotent lookups.
     */
    public void storeResponse(String idempotencyKey, PaymentResponse response) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable — response not cached for key: {}", idempotencyKey);
            return;
        }

        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(key, response, ttlHours, TimeUnit.HOURS);
            
            String lockKey = LOCK_PREFIX + idempotencyKey;
            redisTemplate.delete(lockKey);
            
            log.info("Stored idempotent response for key: {}, paymentId: {}", 
                     idempotencyKey, response.getId());
        } catch (Exception e) {
            log.error("Failed to store idempotent response in Redis: {}", e.getMessage());
        }
    }

    /**
     * Release a processing lock (on failure/error).
     */
    public void releaseLock(String idempotencyKey) {
        if (!isRedisAvailable()) return;

        try {
            String lockKey = LOCK_PREFIX + idempotencyKey;
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("Failed to release idempotency lock: {}", e.getMessage());
        }
    }
}
