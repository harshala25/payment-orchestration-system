# 📋 Test Case Documentation

## Test Classification Strategy

Tests are categorized by importance level:
- **🔴 Sanity** — Must pass before any deployment. Core happy-path flows.
- **🟡 Regression** — Run on every PR. Covers edge cases and integrations.
- **🟢 Integration** — Full end-to-end with real DB/Redis. Run pre-release.
- **⚫ Negative** — Invalid inputs, failures, security. Critical for robustness.

---

## Functional Requirements Coverage

### FR-1: Create Payment API

| # | Test Case | Type | Priority | Expected Result |
|---|-----------|------|----------|-----------------|
| TC-001 | Create CARD payment with valid data | 🔴 Sanity | P0 | Payment created with status SUCCESS/FAILED, card masked |
| TC-002 | Create UPI payment with valid VPA | 🔴 Sanity | P0 | Payment created, routed to Provider B |
| TC-003 | Create payment with duplicate idempotency key | 🟡 Regression | P0 | Returns cached response (HTTP 200), no duplicate charge |
| TC-004 | Create payment with missing required fields | ⚫ Negative | P1 | HTTP 400 with field-level validation errors |
| TC-005 | Create payment with invalid amount (negative) | ⚫ Negative | P1 | HTTP 400: "Amount must be greater than 0" |
| TC-006 | Create payment with invalid currency code | ⚫ Negative | P1 | HTTP 400: "Currency must be 3-letter ISO code" |
| TC-007 | Create payment without API key | ⚫ Negative | P0 | HTTP 401: "Missing API key" |
| TC-008 | Create payment with invalid API key | ⚫ Negative | P0 | HTTP 403: "Invalid API key" |
| TC-009 | Create payment without idempotency key header | ⚫ Negative | P1 | HTTP 400: "Missing header: Idempotency-Key" |
| TC-010 | Create CARD payment — verify card number is masked in response | 🔴 Sanity | P0 | maskedCardNumber = "****-****-****-4242" |
| TC-011 | Create CARD payment — verify CVV is NOT stored/returned | 🔴 Sanity | P0 | No CVV field in response or database |
| TC-012 | Create payment with very large amount (edge case) | 🟡 Regression | P2 | Processes successfully or returns appropriate error |

### FR-2: Fetch Payment API

| # | Test Case | Type | Priority | Expected Result |
|---|-----------|------|----------|-----------------|
| TC-013 | Fetch existing payment by valid UUID | 🔴 Sanity | P0 | Returns payment details with routing attempts |
| TC-014 | Fetch non-existent payment | ⚫ Negative | P1 | HTTP 404: "Payment not found" |
| TC-015 | Fetch payment with invalid UUID format | ⚫ Negative | P2 | HTTP 400: Invalid parameter |
| TC-016 | List payments with pagination | 🟡 Regression | P1 | Returns paginated results with correct page size |
| TC-017 | List payments filtered by status | 🟡 Regression | P1 | Only returns payments matching the filter |
| TC-018 | List payments filtered by merchantId | 🟡 Regression | P1 | Only returns payments for that merchant |

### FR-3: Routing (CARD → A, UPI → B)

| # | Test Case | Type | Priority | Expected Result |
|---|-----------|------|----------|-----------------|
| TC-019 | CARD payment routes to Provider A | 🔴 Sanity | P0 | providerUsed = PROVIDER_A |
| TC-020 | UPI payment routes to Provider B | 🔴 Sanity | P0 | providerUsed = PROVIDER_B |
| TC-021 | No routing rules for payment method | ⚫ Negative | P1 | HTTP 422: "No routing rules configured for WALLET" |
| TC-022 | All providers disabled for a method | ⚫ Negative | P1 | HTTP 422: "No eligible providers available" |
| TC-023 | Weight-based routing distributes traffic | 🟡 Regression | P2 | Over 100 requests, ~70/30 split (within tolerance) |
| TC-024 | Lower priority provider used when primary disabled | 🟡 Regression | P0 | Falls back to secondary provider |

### FR-4: Retry & Failover

| # | Test Case | Type | Priority | Expected Result |
|---|-----------|------|----------|-----------------|
| TC-025 | Soft decline (TIMEOUT) triggers retry via next provider | 🟡 Regression | P0 | attemptCount > 1, may succeed on retry |
| TC-026 | Hard decline (INSUFFICIENT_FUNDS) does NOT retry | 🟡 Regression | P0 | attemptCount = 1, status = FAILED |
| TC-027 | All providers fail → payment marked FAILED | ⚫ Negative | P1 | status = FAILED after exhausting chain |
| TC-028 | Max retries respected (3 attempts max) | 🟡 Regression | P1 | attemptCount <= 3 |
| TC-029 | Provider A down → failover to Provider B | 🟢 Integration | P0 | providerUsed = PROVIDER_B, attemptCount = 2 |
| TC-030 | Circuit breaker opens after 50% failure rate | 🟡 Regression | P2 | Provider marked DEGRADED/DOWN |

### FR-5: Idempotency

| # | Test Case | Type | Priority | Expected Result |
|---|-----------|------|----------|-----------------|
| TC-031 | Duplicate request returns same response | 🔴 Sanity | P0 | Same paymentId and status returned |
| TC-032 | Concurrent duplicate requests handled safely | 🟡 Regression | P0 | Only one payment created (lock mechanism) |
| TC-033 | Different idempotency keys create different payments | 🟡 Regression | P1 | Two distinct payments created |
| TC-034 | Idempotency survives Redis failure (DB constraint) | 🟢 Integration | P1 | DB unique constraint prevents duplicate |

### FR-6: Payment Status Tracking

| # | Test Case | Type | Priority | Expected Result |
|---|-----------|------|----------|-----------------|
| TC-035 | Status transitions: INITIATED → PROCESSING → SUCCESS | 🔴 Sanity | P0 | Final status = SUCCESS |
| TC-036 | Status transitions: INITIATED → PROCESSING → FAILED | 🟡 Regression | P0 | Final status = FAILED with decline reason |
| TC-037 | Refund: SUCCESS → REFUND_INITIATED → REFUNDED | 🟡 Regression | P1 | Final status = REFUNDED |
| TC-038 | Cannot refund a FAILED payment | ⚫ Negative | P1 | HTTP 400: "Can only refund successful payments" |
| TC-039 | Routing attempts timeline recorded | 🟡 Regression | P1 | Each attempt has provider, latency, success/fail |

---

## Non-Functional Requirements Coverage

| # | Test Case | Type | Priority | Expected Result |
|---|-----------|------|----------|-----------------|
| NF-001 | Payment processing latency < 1s (simulated) | 🟡 Regression | P1 | End-to-end < 1000ms |
| NF-002 | API responds within 5s under normal load | 🟡 Regression | P1 | 95th percentile < 5s |
| NF-003 | System handles 100 concurrent requests | 🟢 Integration | P2 | No errors, all requests processed |
| NF-004 | Audit log captures all payment state changes | 🟡 Regression | P0 | Complete audit trail in DB |
| NF-005 | Card data never stored in plaintext | 🔴 Sanity | P0 | Only masked values in DB + response |
| NF-006 | API returns consistent error format | 🟡 Regression | P1 | All errors follow ApiErrorResponse schema |
| NF-007 | OpenAPI spec accessible without auth | 🟡 Regression | P2 | Swagger UI loads at /swagger-ui.html |
| NF-008 | CORS headers set for Angular frontend | 🟡 Regression | P2 | Angular on :4200 can call API on :8080 |

---

## Test Execution Summary

| Category | Total | Automated | Manual |
|----------|-------|-----------|--------|
| Sanity | 8 | 8 | 0 |
| Regression | 19 | 15 | 4 |
| Integration | 4 | 2 | 2 |
| Negative | 12 | 12 | 0 |
| **Total** | **43** | **37** | **6** |

### Running Automated Tests

```bash
cd backend
mvn test                    # All unit tests
mvn test -Dtest=*Sanity*    # Sanity tests only
mvn verify                  # Unit + integration tests
```
