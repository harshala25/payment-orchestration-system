# 📡 API Documentation

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
All endpoints require the `X-API-Key` header:
```
X-API-Key: pgw_live_sk_yuno_2026_assessment_key
```

---

## Endpoints Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/payments` | Create a new payment |
| `GET` | `/payments/{id}` | Fetch payment by ID |
| `GET` | `/payments` | List payments (paginated) |
| `POST` | `/payments/{id}/refund` | Refund a payment |
| `GET` | `/routing/rules` | List routing rules |
| `POST` | `/routing/rules` | Create a routing rule |
| `GET` | `/routing/providers` | List providers |
| `GET` | `/routing/providers/{code}/health` | Provider health summary |
| `PUT` | `/routing/providers/{code}/toggle` | Enable/disable provider |
| `GET` | `/routing/approval-rates` | Approval rates |
| `GET` | `/analytics/dashboard` | Dashboard analytics |
| `GET` | `/compliance/audit-log` | Audit trail |
| `GET` | `/compliance/security-report` | Security report |

---

## 1. Create Payment

### Request
```http
POST /api/v1/payments
Content-Type: application/json
X-API-Key: pgw_live_sk_yuno_2026_assessment_key
Idempotency-Key: unique-key-001
```

#### Card Payment Body
```json
{
  "merchantId": "merchant_001",
  "amount": 1500.00,
  "currency": "INR",
  "paymentMethod": "CARD",
  "cardNumber": "4242424242424242",
  "cardExpiryMonth": "12",
  "cardExpiryYear": "2028",
  "cardCvv": "123",
  "cardHolderName": "John Doe",
  "description": "Order #12345",
  "customerEmail": "john@example.com",
  "customerName": "John Doe",
  "metadata": "{\"orderId\": \"ORD-001\"}"
}
```

#### UPI Payment Body
```json
{
  "merchantId": "merchant_001",
  "amount": 500.00,
  "currency": "INR",
  "paymentMethod": "UPI",
  "upiVpa": "user@paytm",
  "description": "UPI Payment",
  "customerEmail": "user@example.com"
}
```

### Success Response (201 Created)
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "merchantId": "merchant_001",
  "amount": 1500.00,
  "currency": "INR",
  "paymentMethod": "CARD",
  "status": "SUCCESS",
  "providerUsed": "PROVIDER_A",
  "providerTransactionId": "TXN-A-1234ABCD5678",
  "maskedCardNumber": "****-****-****-4242",
  "cardBrand": "VISA",
  "description": "Order #12345",
  "attemptCount": 1,
  "routingAttempts": [
    {
      "attemptNumber": 1,
      "providerCode": "PROVIDER_A",
      "success": true,
      "latencyMs": 120,
      "createdAt": "2026-05-16T12:00:00"
    }
  ],
  "createdAt": "2026-05-16T12:00:00",
  "updatedAt": "2026-05-16T12:00:00"
}
```

### Failed Response (201 Created — payment processed but declined)
```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "status": "FAILED",
  "providerUsed": "PROVIDER_A",
  "declineReason": "INSUFFICIENT_FUNDS",
  "declineMessage": "Insufficient funds in account",
  "attemptCount": 1,
  "routingAttempts": [
    {
      "attemptNumber": 1,
      "providerCode": "PROVIDER_A",
      "success": false,
      "declineReason": "INSUFFICIENT_FUNDS",
      "latencyMs": 150
    }
  ]
}
```

### Failover Response (retry succeeded on Provider B)
```json
{
  "status": "SUCCESS",
  "providerUsed": "PROVIDER_B",
  "attemptCount": 2,
  "routingAttempts": [
    {
      "attemptNumber": 1,
      "providerCode": "PROVIDER_A",
      "success": false,
      "declineReason": "TIMEOUT",
      "latencyMs": 3000
    },
    {
      "attemptNumber": 2,
      "providerCode": "PROVIDER_B",
      "success": true,
      "latencyMs": 85
    }
  ]
}
```

### Idempotent Response (200 OK — duplicate request)
Returns the exact same response as the original request.

### Validation Error (400)
```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "traceId": "abc-123-def",
  "fieldErrors": [
    { "field": "amount", "message": "Amount must be greater than 0", "rejectedValue": -10 },
    { "field": "currency", "message": "Currency must be a 3-letter ISO code", "rejectedValue": "INVALID" }
  ],
  "timestamp": "2026-05-16T12:00:00"
}
```

---

## 2. Fetch Payment

```http
GET /api/v1/payments/a1b2c3d4-e5f6-7890-abcd-ef1234567890
X-API-Key: pgw_live_sk_yuno_2026_assessment_key
```

### Response (200 OK)
Same schema as Create Payment response.

### Not Found (404)
```json
{
  "status": 404,
  "error": "PAYMENT_NOT_FOUND",
  "message": "Payment not found: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "traceId": "xyz-789"
}
```

---

## 3. List Payments

```http
GET /api/v1/payments?page=0&size=20&status=SUCCESS&merchantId=merchant_001
X-API-Key: pgw_live_sk_yuno_2026_assessment_key
```

### Response (200 OK)
```json
{
  "content": [ /* array of PaymentResponse */ ],
  "totalElements": 47,
  "totalPages": 3,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

---

## 4. Refund Payment

```http
POST /api/v1/payments/a1b2c3d4-e5f6-7890-abcd-ef1234567890/refund
X-API-Key: pgw_live_sk_yuno_2026_assessment_key
```

### Response (200 OK)
```json
{
  "status": "REFUNDED",
  "providerUsed": "PROVIDER_A"
}
```

---

## 5. Toggle Provider

```http
PUT /api/v1/routing/providers/PROVIDER_A/toggle?enabled=false
X-API-Key: pgw_live_sk_yuno_2026_assessment_key
```

Disabling a provider will cause the routing engine to skip it, testing failover behavior.

---

## 6. Dashboard Analytics

```http
GET /api/v1/analytics/dashboard?hoursBack=24
X-API-Key: pgw_live_sk_yuno_2026_assessment_key
```

### Response (200 OK)
```json
{
  "totalTransactions": 1247,
  "successfulTransactions": 1098,
  "failedTransactions": 149,
  "overallSuccessRate": 88.05,
  "totalVolume": 1247000.00,
  "currency": "INR",
  "paymentMethodBreakdown": [
    { "paymentMethod": "CARD", "totalCount": 723, "successCount": 645, "approvalRate": 89.2 },
    { "paymentMethod": "UPI", "totalCount": 524, "successCount": 453, "approvalRate": 86.4 }
  ],
  "providerBreakdown": [
    {
      "providerCode": "PROVIDER_A", "providerName": "CardPay Global",
      "healthStatus": "HEALTHY", "isEnabled": true,
      "totalCount": 723, "successCount": 645, "approvalRate": 89.2,
      "avgLatencyMs": 120, "p95LatencyMs": 250, "p99LatencyMs": 290
    }
  ],
  "topDeclineReasons": [
    { "reason": "INSUFFICIENT_FUNDS", "description": "Insufficient funds in account", "count": 63, "percentage": 42.3, "isRetryable": false },
    { "reason": "TIMEOUT", "description": "Provider did not respond in time", "count": 42, "percentage": 28.2, "isRetryable": true }
  ]
}
```

---

## Error Codes Reference

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request body validation failed |
| `MISSING_HEADER` | 400 | Required header not provided |
| `AUTHENTICATION_ERROR` | 401 | Missing API key |
| `AUTHENTICATION_ERROR` | 403 | Invalid API key |
| `PAYMENT_NOT_FOUND` | 404 | Payment ID doesn't exist |
| `PAYMENT_DECLINED` | 402 | Hard decline from provider |
| `ROUTING_ERROR` | 422 | No routing rules or eligible providers |
| `PROVIDER_UNAVAILABLE` | 503 | All providers are down |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## Payment Status Values

| Status | Description | Terminal? |
|--------|-------------|-----------|
| `INITIATED` | Payment created, not yet sent to provider | No |
| `PROCESSING` | Sent to provider, awaiting response | No |
| `SUCCESS` | Provider confirmed approval | Yes |
| `FAILED` | Provider declined or all retries exhausted | Yes |
| `RETRY` | Marked for retry via next provider | No |
| `REFUND_INITIATED` | Refund request sent | No |
| `REFUNDED` | Refund confirmed | Yes |
| `CANCELLED` | Cancelled before processing | Yes |

---

## Interactive Documentation

For interactive API testing with auto-generated schemas:
```
http://localhost:8080/swagger-ui.html
```
