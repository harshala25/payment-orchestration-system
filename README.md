# Payment Orchestration System

> A production-ready, high-performance Payment Orchestration System built for the Yuno Core Assessment.
> It unifies multiple payment gateways under a single integration, offering dynamic routing, exponential backoff failover retries, absolute double-tier idempotency, PCI-DSS compliant masking, and real-time dashboard analytics.

---

## Key Features

*   **Unified API Integration**: Connect to multiple simulated payment providers through a single normalized endpoint.
*   **Dynamic Smart Routing**: Run rule-based, priority-based, or weighted A/B split traffic routing dynamically configured via PostgreSQL.
*   **Failover & Resilient Retries**: Automatically handle soft failures, retry payments with exponential backoff, and transparently fall back to healthier gateways.
*   **Double-Tier Idempotency**: Bulletproof transaction protection using Redis TTL caching followed by PostgreSQL unique lock constraints.
*   **PCI-DSS & Security Best Practices**: Mask sensitive PAN data (always storing last 4 digits only, CVV never persistent) and secure requests with API key filters and HSTS headers.
*   **Enterprise Dashboard**: A beautiful, real-time Angular interface aggregating volume, approval rates, decline distributions, and gateway performance.

---

## Technology Stack

*   **Backend Core**: Java 17, Spring Boot 3.2.5, Spring Data JPA, Spring Security
*   **Databases & Caching**: PostgreSQL 16, H2 Database (In-Memory Fallback), Redis (State Cache & Idempotency)
*   **Frontend**: Angular 18 (Standalone architecture)
*   **Documentation & Specs**: OpenAPI 3.0 (Swagger UI)

---

## Architecture

```
                       [ Merchant Client / Frontend ]
                                      │  (HTTPS + X-API-Key)
                                      ▼
                      ┌───────────────────────────────┐
                      │    API Gateway & Security     │
                      │  (API Key & Masking Filters)  │
                      └───────────────┬───────────────┘
                                      │
                                      ▼
                      ┌───────────────────────────────┐
                      │  Payment Orchestration Core   │
                      │  (Idempotency & Lifecycle)    │
                      └───────────────┬───────────────┘
                                      │
               ┌──────────────────────┴──────────────────────┐
               ▼                                             ▼
┌──────────────────────────────┐              ┌──────────────────────────────┐
│    Dynamic Routing Engine    │              │    Analytics & Monitoring    │
│  (Priority + Weight Rules)   │              │  (Provider Performance, p95) │
└──────────────┬───────────────┘              └──────────────┬───────────────┘
               │                                             │
               ▼                                             ▼
┌──────────────────────────────┐              ┌──────────────────────────────┐
│     Provider Connectors      │              │      Postgres & Redis        │
│  (Provider A / Provider B)   │              │  (Audit Log & Cache Layer)   │
└──────────────────────────────┘              └──────────────────────────────┘
```

---

## Quick Start & Execution

### 1. Prerequisite Checklist
*   **Java**: JDK 17 installed and set on your classpath.
*   **Node.js**: v18.x or above (for Frontend).
*   **Docker/Local instances**: PostgreSQL and Redis.
    (Note: If Postgres or Redis are not available on your machine, the system dynamically switches to an In-Memory H2 fallback mode to allow immediate running and evaluation)

### 2. Database Creation
If running locally, connect to your PostgreSQL instance and create the target database:
```sql
CREATE DATABASE payment_orchestration_system;
```

### 3. Spin Up the Backend
Navigate to the backend directory and run:
```bash
cd backend
# Set timezones to UTC for consistent audit logging
export JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
# Execute using Maven Wrapper
./mvnw spring-boot:run
```
*   **OpenAPI Documentation**: Swagger UI is accessible at `http://localhost:8080/swagger-ui.html`
*   **Dev Mode (No Local Postgres/Redis)**: You can launch with the `dev` profile to use in-memory database mocks automatically:
    `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

### 4. Spin Up the Frontend Dashboard
Open a new terminal and run:
```bash
cd frontend
npm install
npm start
```
*   **Dashboard URL**: `http://localhost:4200/dashboard`

---

## Testing

The backend is backed by an exhaustive JUnit 5 + Mockito unit and integration test suite, capturing negative paths, gateway time-outs, and concurrency locks:
```bash
cd backend
./mvnw clean test
```

---

## Document Registry

For deep-dive evaluations, look at the detailed documentation in the `docs` directory:

1.  **[API Reference](file:///docs/API-DOCUMENTATION.md)**: Exhaustive endpoint payloads, authorization structure, schema designs, and quick curl payloads.
2.  **[System Architecture Design](file:///docs/ARCHITECTURE.md)**: Detailed entity-relationship models, schema designs, thread-safe transactional management, and performance considerations.
3.  **[Test Cases Specification](file:///docs/TEST-CASES.md)**: Categorized lists of test suites including Sanity, Regression, Integration, and negative paths with detailed descriptions.
