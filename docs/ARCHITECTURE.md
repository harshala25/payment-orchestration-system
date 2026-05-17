# 🏛️ Architecture Documentation

## System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                        │
│   Angular Dashboard (localhost:4200)  │  External APIs (cURL/Postman)  │
└───────────────────────┬────────────────────────────────────────────────┘
                        │ HTTPS / REST
                        ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT APPLICATION (port 8080)                 │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    SECURITY LAYER                                 │  │
│  │  ApiKeyAuthFilter → validates X-API-Key header                   │  │
│  │  CORS Filter → allows Angular origin                             │  │
│  │  OWASP Headers → HSTS, X-Frame-Options, X-Content-Type-Options  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                        │                                               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    CONTROLLER LAYER                               │  │
│  │  PaymentController    → POST/GET /api/v1/payments                │  │
│  │  RoutingController    → GET/POST /api/v1/routing                 │  │
│  │  AnalyticsController  → GET /api/v1/analytics                    │  │
│  │  ComplianceController → GET /api/v1/compliance                   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                        │                                               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    SERVICE LAYER                                  │  │
│  │                                                                   │  │
│  │  PaymentOrchestrationService ──┐ (Central Coordinator)           │  │
│  │       │                        │                                  │  │
│  │       ├── IdempotencyService ──┤ Redis SETNX + TTL               │  │
│  │       │                        │                                  │  │
│  │       ├── RoutingEngineService ┤ Rule-based + Weight-based       │  │
│  │       │                        │                                  │  │
│  │       ├── ComplianceService ───┤ Async audit logging             │  │
│  │       │                        │                                  │  │
│  │       ├── ApprovalRateService ─┤ Scheduled health evaluation     │  │
│  │       │                        │                                  │  │
│  │       └── AnalyticsService ────┘ Aggregation + percentiles       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                        │                                               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    PROVIDER CONNECTOR LAYER                       │  │
│  │                                                                   │  │
│  │  ProviderConnector (Interface)                                    │  │
│  │       │                                                           │  │
│  │       ├── ProviderAConnector  (Card Gateway - simulated)          │  │
│  │       │     85% success rate, 50-300ms latency                    │  │
│  │       │     Decline: insufficient_funds, card_expired, timeout    │  │
│  │       │                                                           │  │
│  │       └── ProviderBConnector  (UPI Gateway - simulated)           │  │
│  │             90% success rate, 30-200ms latency                    │  │
│  │             Decline: vpa_not_found, bank_unavailable, timeout     │  │
│  │                                                                   │  │
│  │  ProviderConnectorFactory (auto-discovery via Spring DI)          │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                        │                                               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    DATA LAYER                                     │  │
│  │                                                                   │  │
│  │  Spring Data JPA Repositories                                     │  │
│  │       │                                                           │  │
│  │       ├── PaymentRepository                                       │  │
│  │       ├── RoutingAttemptRepository                                │  │
│  │       ├── RoutingRuleRepository                                   │  │
│  │       ├── ProviderRepository                                      │  │
│  │       ├── AuditLogRepository                                      │  │
│  │       └── TransactionMetricRepository                             │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
                        │                        │
                        ▼                        ▼
┌──────────────────────────────┐  ┌────────────────────────────────────┐
│      PostgreSQL              │  │           Redis 7.x               │
│                              │  │                                    │
│  Tables:                     │  │  Keys:                             │
│  • payments                  │  │  • idempotency:{key} → response   │
│  • routing_attempts          │  │    (TTL: 24 hours)                 │
│  • routing_rules             │  │                                    │
│  • providers                 │  │  • idempotency_lock:{key}          │
│  • audit_logs                │  │    → "PROCESSING"                  │
│  • transaction_metrics       │  │    (TTL: 60 seconds)               │
│                              │  │                                    │
│  Indexes:                    │  │  Purpose:                          │
│  • idx_payment_idempotency   │  │  • Fast duplicate detection        │
│  • idx_payment_status        │  │  • Distributed processing lock     │
│  • idx_payment_method        │  │  • Response caching                │
│  • idx_metric_provider_time  │  │                                    │
│  • idx_audit_timestamp       │  │                                    │
└──────────────────────────────┘  └────────────────────────────────────┘
```

---

## Payment Processing Flow

```
                    ┌─────────────┐
                    │   Client    │
                    │  Request    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  API Key    │──── ❌ 401 Unauthorized
                    │  Validate   │
                    └──────┬──────┘
                           │ ✅
                    ┌──────▼──────┐
                    │  Request    │──── ❌ 400 Validation Error
                    │  Validate   │
                    └──────┬──────┘
                           │ ✅
                    ┌──────▼──────┐     ┌─────────────┐
                    │ Idempotency │────▶│ Redis Cache  │
                    │   Check     │     └──────┬──────┘
                    └──────┬──────┘            │
                           │                   │ HIT → Return cached response
                           │ MISS              │
                    ┌──────▼──────┐            │
                    │  Acquire    │◄───────────┘
                    │  Redis Lock │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Create    │──── PostgreSQL INSERT
                    │  Payment    │     (status: INITIATED)
                    │  Record     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Routing    │──── Query routing_rules table
                    │  Engine     │     Filter by health + enabled
                    │  Resolve    │     Return failover chain
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
              ┌────▶│  Provider   │
              │     │  Attempt    │
              │     └──────┬──────┘
              │            │
              │     ┌──────▼──────┐
              │     │  Response?  │
              │     └──┬───┬───┬──┘
              │        │   │   │
              │    SUCCESS │  SOFT     HARD
              │        │   │ DECLINE  DECLINE
              │        │   │   │        │
              │        │   │   │  ┌─────▼─────┐
              │        │   │   │  │   FAIL     │
              │        │   │   │  │ Immediately│
              │        │   │   │  └────────────┘
              │        │   │   │
              │        │   └───┤
              │        │       │
              │        │  ┌────▼────┐
              │        │  │  Next   │
              └────────┤  │Provider?│
                       │  └────┬────┘
                       │       │ NO
                       │  ┌────▼────┐
                       │  │  FAIL   │
                       │  │  All    │
                       │  │exhausted│
                       │  └─────────┘
                       │
                ┌──────▼──────┐
                │  Record     │──── transaction_metrics
                │  Metrics    │──── audit_logs
                └──────┬──────┘
                       │
                ┌──────▼──────┐
                │  Cache in   │──── Redis SET with TTL
                │  Redis      │
                └──────┬──────┘
                       │
                ┌──────▼──────┐
                │  Return     │
                │  Response   │
                └─────────────┘
```

---

## Database Schema (ERD)

```
┌──────────────────────────────┐
│         payments             │
├──────────────────────────────┤
│ id           UUID (PK)       │
│ idempotency_key  VARCHAR(UQ) │
│ merchant_id  VARCHAR         │
│ amount       DECIMAL(19,4)   │
│ currency     VARCHAR(3)      │
│ payment_method ENUM          │
│ status       ENUM            │
│ provider_used ENUM           │
│ provider_txn_id VARCHAR      │
│ decline_reason ENUM          │
│ masked_card_number VARCHAR   │
│ card_brand   VARCHAR         │
│ upi_vpa      VARCHAR         │
│ attempt_count INT            │
│ version      BIGINT (OL)     │
│ created_at   TIMESTAMP       │
│ updated_at   TIMESTAMP       │
├──────────────────────────────┤
│ 1 ──── N  routing_attempts   │
└──────────────────────────────┘

┌──────────────────────────────┐
│     routing_attempts         │
├──────────────────────────────┤
│ id           UUID (PK)       │
│ payment_id   UUID (FK)       │
│ attempt_number INT           │
│ provider_code ENUM           │
│ success      BOOLEAN         │
│ decline_reason ENUM          │
│ latency_ms   BIGINT          │
│ provider_txn_id VARCHAR      │
│ raw_response TEXT            │
│ created_at   TIMESTAMP       │
└──────────────────────────────┘

┌──────────────────────────────┐     ┌──────────────────────────────┐
│      routing_rules           │     │        providers             │
├──────────────────────────────┤     ├──────────────────────────────┤
│ id           UUID (PK)       │     │ id           UUID (PK)       │
│ payment_method ENUM          │     │ name         VARCHAR         │
│ provider_code ENUM           │     │ code         ENUM (UQ)       │
│ priority     INT             │     │ is_enabled   BOOLEAN         │
│ weight       INT (0-100)     │     │ health_status VARCHAR        │
│ is_active    BOOLEAN         │     │ approval_rate DOUBLE         │
│ description  VARCHAR         │     │ total_txns   BIGINT          │
│ created_at   TIMESTAMP       │     │ avg_latency  DOUBLE          │
│ updated_at   TIMESTAMP       │     │ last_health_check TIMESTAMP  │
└──────────────────────────────┘     └──────────────────────────────┘

┌──────────────────────────────┐     ┌──────────────────────────────┐
│       audit_logs             │     │   transaction_metrics        │
├──────────────────────────────┤     ├──────────────────────────────┤
│ id           UUID (PK)       │     │ id           UUID (PK)       │
│ entity_type  VARCHAR         │     │ payment_id   UUID            │
│ entity_id    VARCHAR         │     │ provider_code ENUM           │
│ action       VARCHAR         │     │ payment_method ENUM          │
│ old_value    TEXT             │     │ status       ENUM            │
│ new_value    TEXT             │     │ amount       DECIMAL         │
│ merchant_id  VARCHAR         │     │ latency_ms   BIGINT          │
│ performed_by VARCHAR         │     │ decline_reason ENUM          │
│ trace_id     VARCHAR         │     │ was_retried  BOOLEAN         │
│ timestamp    TIMESTAMP       │     │ was_failover BOOLEAN         │
└──────────────────────────────┘     │ timestamp    TIMESTAMP       │
                                     └──────────────────────────────┘
```

---

## Key Design Patterns

| Pattern | Where Used | Purpose |
|---------|-----------|---------|
| **Strategy** | `ProviderConnector` interface | Each provider is a pluggable strategy |
| **Factory** | `ProviderConnectorFactory` | Auto-discovers connectors via Spring DI |
| **Template Method** | `PaymentOrchestrationService` | Fixed pipeline: validate → route → execute → record |
| **Observer** | `ComplianceService` (async) | Reacts to state changes without blocking |
| **Circuit Breaker** | Resilience4j per provider | Prevents cascading failures |
| **Two-Phase Lock** | Redis lock + DB constraint | Idempotency race condition prevention |
| **CQRS-lite** | `TransactionMetric` table | Separate read model for fast analytics |
| **State Machine** | `PaymentStatus` enum | Enforced lifecycle transitions |
| **Builder** | Lombok `@Builder` on entities/DTOs | Clean object construction |
| **Repository** | Spring Data JPA | Data access abstraction |

---

## Resilience Patterns

### Idempotency (Two-Layer)
```
Request ──▶ Redis GET ──▶ HIT? ──▶ Return cached response
                │
                ▼ MISS
           Redis SETNX (lock, 60s TTL)
                │
                ▼ Lock acquired
           Process payment
                │
                ▼
           Redis SET (response, 24h TTL)
           Release lock
                │
                ▼
           PostgreSQL UNIQUE constraint (final safety net)
```

### Retry/Failover (Smart Decline Handling)
```
Provider response ──▶ Success? ──▶ YES ──▶ Return SUCCESS
                          │
                          ▼ NO
                    Hard decline? ──▶ YES ──▶ Return FAILED (no retry)
                    (INSUFFICIENT_FUNDS,       │
                     CARD_EXPIRED, etc.)       │
                          │                    │
                          ▼ NO (Soft decline)  │
                    More providers? ──▶ YES ──▶ Try next provider
                          │
                          ▼ NO
                    Return FAILED (all exhausted)
```

### Circuit Breaker (Per Provider)
```
CLOSED ──[failure rate < 50%]──▶ CLOSED (normal operation)
  │
  ├──[failure rate ≥ 50%]──▶ OPEN (reject all, wait 30s)
  │                              │
  │                        ──▶ HALF-OPEN (allow 3 test calls)
  │                              │
  │                    ──[tests pass]──▶ CLOSED
  │                    ──[tests fail]──▶ OPEN
```

---

## Security Architecture

| Layer | Implementation | Purpose |
|-------|---------------|---------|
| **Authentication** | API Key (`X-API-Key` header) | Identify merchant |
| **Integrity** | HMAC-SHA256 signing (utility ready) | Prevent request tampering |
| **Transport** | HSTS header (enforces HTTPS) | Encrypt in transit |
| **Data Protection** | Card masking (PCI-DSS) | Protect sensitive data |
| **Audit** | Immutable audit log | Compliance & forensics |
| **Headers** | OWASP security headers | Prevent common web attacks |
| **Session** | Stateless (no cookies/sessions) | No session hijacking risk |
| **CORS** | Origin-restricted | Prevent unauthorized frontends |
