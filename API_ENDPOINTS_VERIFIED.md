# ✅ Verified API Endpoints - Payment Gateway

**Base URL:** `http://localhost:8080/api/v1`

---

## 🔐 Authentication Endpoints

### Register User
```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "Admin@123",
  "firstName": "Admin",
  "lastName": "User"
}
```

### Login
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "Admin@123"
}

Response:
{
  "token": "eyJhbGc...",
  "expiresIn": 86400000
}
```

---

## 👥 Customer Management Endpoints

### Create Customer Profile
```bash
POST /api/v1/api/customers/profiles
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "customerId": "CUST001",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1-555-123-4567"
}

Response includes: customerProfileId
```

### Add Payment Profile to Customer
```bash
POST /api/v1/api/customers/profiles/{customerProfileId}/payment-profiles
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "cardNumber": "4111111111111111",
  "expiryMonth": "12",
  "expiryYear": "2028",
  "cvv": "123",
  "billingAddress": {
    "firstName": "John",
    "lastName": "Doe",
    "address": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zip": "10001",
    "country": "US"
  }
}

Response includes: paymentProfileId
```

---

## 💳 Payment Transaction Endpoints

### Purchase (Auth + Capture)
```bash
POST /api/v1/payments/purchase
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "amount": 99.99,
  "currency": "USD",
  "customerId": "CUST001",
  "description": "Test Purchase - Product ABC",
  "invoiceNumber": "INV-001",
  "paymentMethod": {
    "cardNumber": "4111111111111111",
    "expiryMonth": "12",
    "expiryYear": "2028",
    "cvv": "123",
    "cardType": "CREDIT_CARD"
  }
}

Response includes: transactionId, status, authCode
```

### Authorize Only
```bash
POST /api/v1/payments/authorize
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "amount": 149.99,
  "currency": "USD",
  "customerId": "CUST001",
  "description": "Test Authorization - Service XYZ",
  "invoiceNumber": "INV-002",
  "paymentMethod": {
    "cardNumber": "4111111111111111",
    "expiryMonth": "12",
    "expiryYear": "2028",
    "cvv": "123",
    "cardType": "CREDIT_CARD"
  }
}

Response includes: transactionId (save for capture)
```

### Capture Authorized Payment
```bash
POST /api/v1/payments/capture
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "transactionId": "TRANSACTION_ID_FROM_AUTHORIZE",
  "amount": 149.99
}
```

### Refund Transaction
```bash
POST /api/v1/payments/refund
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "transactionId": "TRANSACTION_ID_FROM_PURCHASE",
  "amount": 49.99,
  "currency": "USD",
  "description": "Customer requested partial refund"
}
```

---

## 📅 Subscription Plan Endpoints

### Create Subscription Plan
```bash
POST /api/v1/subscription-plans
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "planCode": "premium_monthly",
  "name": "Premium Monthly Plan",
  "description": "Premium features with monthly billing",
  "amount": 29.99,
  "currency": "USD",
  "intervalUnit": "MONTH",
  "intervalCount": 1,
  "trialPeriodDays": 7,
  "isActive": true
}

Response includes: planCode, planId
```

### Get Plan by Code
```bash
GET /api/v1/subscription-plans/{planCode}
Authorization: Bearer {JWT_TOKEN}
```

### List All Active Plans
```bash
GET /api/v1/subscription-plans/active
Authorization: Bearer {JWT_TOKEN}
```

---

## 🔄 Subscription Management Endpoints

### Create Subscription
```bash
POST /api/v1/subscriptions
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "customerId": "CUST002",
  "planCode": "premium_monthly",
  "paymentMethodId": "PAYMENT_PROFILE_ID",
  "startTrial": true,
  "prorated": true,
  "description": "Premium subscription for customer"
}

Response includes: subscriptionId, status, nextBillingDate
```

### Get Subscription by ID
```bash
GET /api/v1/subscriptions/{subscriptionId}
Authorization: Bearer {JWT_TOKEN}
```

### Get Customer's Subscriptions
```bash
GET /api/v1/subscriptions/customer/{customerId}
Authorization: Bearer {JWT_TOKEN}
```

### Cancel Subscription
```bash
POST /api/v1/subscriptions/{subscriptionId}/cancel
Authorization: Bearer {JWT_TOKEN}
```

---

## 🔍 Health & Status Endpoints

### Health Check
```bash
GET /api/v1/health
```

### Actuator Health (if enabled)
```bash
GET /api/v1/actuator/health
```

---

## 📝 Important Notes

### Authorization Header
All endpoints (except auth endpoints and health) require:
```
Authorization: Bearer {JWT_TOKEN}
```

### Context Path
- Application uses `/api/v1` as the context path
- All endpoints are prefixed with this path
- CustomerController has additional `/api` prefix: `/api/v1/api/customers`

### Test Credit Cards (Authorize.Net Sandbox)
- Visa: `4111111111111111`
- Mastercard: `5424000000000015`
- Amex: `378282246310005`
- Discover: `6011000000000012`

### Response Format
All responses include:
- `correlationId`: For request tracking
- Standard error format with `code`, `message`, `description`, `category`

### Common HTTP Status Codes
- `200` - Success
- `201` - Created
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (invalid/missing JWT)
- `404` - Resource Not Found
- `409` - Conflict (duplicate, constraint violation)
- `422` - Unprocessable Entity (payment declined)
- `500` - Internal Server Error

---

**Generated:** 2026-01-05  
**Application Version:** Payment Gateway v1.0.0  
**Spring Boot:** 3.2+  
**Java:** 17
