<div align="center">

# 💳 Payment Gateway Integration Platform

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=java" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot 3.2.5"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15+-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL 15+"/>
  <img src="https://img.shields.io/badge/Redis-7.0+-red?style=for-the-badge&logo=redis" alt="Redis 7.0+"/>
  <img src="https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker" alt="Docker"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Build-Passing-success?style=flat-square" alt="Build Status"/>
  <img src="https://img.shields.io/badge/Coverage-85%25-brightgreen?style=flat-square" alt="Test Coverage"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"/>
  <img src="https://img.shields.io/badge/PRs-Welcome-brightgreen?style=flat-square" alt="PRs Welcome"/>
</p>

### 🚀 A comprehensive, enterprise-grade payment processing solution built with Spring Boot 3.x and Authorize.Net integration, designed for scalability, security, and reliability.

---

</div>

## 📋 Table of Contents

- [✨ Features](#-features)
- [🏗️ Architecture](#%EF%B8%8F-architecture)
- [🎯 Quick Start](#-quick-start)
- [🛠️ Prerequisites](#%EF%B8%8F-prerequisites)
- [📦 Installation](#-installation)
- [🗄️ Database Setup](#%EF%B8%8F-database-setup)
- [🚀 Running the Application](#-running-the-application)
- [⚙️ Background Workers](#%EF%B8%8F-background-workers)
- [🔧 Configuration](#-configuration)
- [🧪 Testing](#-testing)
- [📊 Monitoring](#-monitoring)
- [🔐 Security](#-security)
- [🚀 Deployment](#-deployment)
- [📚 API Documentation](#-api-documentation)
- [🐛 Troubleshooting](#-troubleshooting)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

## ✨ Features

<details>
<summary>🔥 <strong>Core Payment Processing</strong> - Click to expand</summary>

- 💳 **Purchase Transactions** - One-time payment processing
- 🔒 **Authorize & Capture** - Two-step payment authorization
- ❌ **Void Operations** - Cancel authorized transactions
- 💰 **Refund Processing** - Full and partial refunds
- 🔄 **Idempotent Operations** - Safe retry mechanisms
- 📝 **Transaction Logging** - Comprehensive audit trails

</details>

<details>
<summary>📅 <strong>Subscription Management</strong> - Click to expand</summary>

- 🔄 **Recurring Payments** - Automated subscription billing
- 📊 **Billing Cycles** - Flexible payment schedules
- 🎯 **Subscription Analytics** - Revenue and churn metrics
- 🔔 **Payment Notifications** - Success/failure alerts
- ⏱️ **Retry Logic** - Failed payment recovery

</details>

<details>
<summary>🛡️ <strong>Security & Authentication</strong> - Click to expand</summary>

- 🔐 **JWT Authentication** - Secure token-based auth
- 🗝️ **API Key Management** - Service-to-service authentication
- 🚦 **Rate Limiting** - Redis-backed request throttling
- 🔒 **Data Encryption** - At-rest and in-transit encryption
- 🛡️ **Input Validation** - SQL injection prevention
- 📋 **Security Headers** - HSTS, CSP, and more

</details>

<details>
<summary>📊 <strong>Observability & Monitoring</strong> - Click to expand</summary>

- 📈 **Prometheus Metrics** - Custom business metrics
- 🔍 **Distributed Tracing** - Request correlation tracking
- ❤️ **Health Checks** - Database, Redis, and service health
- 📋 **Structured Logging** - JSON logs with correlation IDs
- 🚨 **Alerting** - Grafana dashboard and alerts
- 📊 **Performance Monitoring** - Response times and throughput

</details>

<details>
<summary>🌐 <strong>Integration & Webhooks</strong> - Click to expand</summary>

- 🔗 **Webhook Processing** - Async event handling
- 🔄 **Retry Mechanisms** - Exponential backoff strategies
- 📨 **Event Notifications** - Real-time payment updates
- 🎯 **Signature Verification** - Secure webhook validation
- 📊 **Event Tracking** - Webhook delivery analytics

</details>

<details>
<summary>🏗️ <strong>DevOps & Infrastructure</strong> - Click to expand</summary>

- 🐳 **Docker Support** - Containerized deployment
- ☸️ **Kubernetes Ready** - Cloud-native deployment
- 🌍 **Multi-Environment** - Dev, staging, and production configs
- 🔄 **CI/CD Ready** - Automated testing and deployment
- 📦 **Maven Build** - Dependency management
- 🗄️ **Database Migrations** - Flyway schema management

</details>

## 🏗️ Architecture

### Technology Stack

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| **Framework** | Spring Boot | 3.2.5 | Core application framework |
| **Language** | Java | 17+ | Programming language |
| **Database** | PostgreSQL | 15+ | Primary data storage |
| **Cache** | Redis | 7.0+ | Caching and session management |
| **Payment Gateway** | Authorize.Net | 2.0.4+ | Payment processing |
| **Build Tool** | Maven | 3.9+ | Dependency management |
| **Containerization** | Docker | Latest | Application deployment |
| **Monitoring** | Micrometer + Prometheus | Latest | Metrics and monitoring |

### Project Structure

```
payment-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/talentica/paymentgateway/
│   │   │   ├── controller/          # REST API controllers
│   │   │   ├── service/             # Business logic services
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── entity/              # JPA entities
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── exception/           # Exception handling
│   │   │   └── util/                # Utility classes
│   │   └── resources/
│   │       ├── db/migration/        # Flyway database migrations
│   │       ├── application.yml      # Main configuration
│   │       ├── application-dev.yml  # Development configuration
│   │       ├── application-staging.yml # Staging configuration
│   │       └── application-prod.yml # Production configuration
│   └── test/                        # Test classes
├── docker/                          # Docker configuration files
├── k8s/                             # Kubernetes manifests
├── monitoring/                      # Grafana & Prometheus configs
├── scripts/                         # Utility scripts
├── pom.xml                          # Maven configuration
├── Dockerfile                       # Docker image definition
├── docker-compose.yml               # Local development environment
└── README.md                        # This file
```

---

## 🎯 Quick Start

> **🚀 Get up and running in under 5 minutes!**

### Option 1: Docker Compose (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/your-org/payment-gateway.git
cd payment-gateway

# 2. Start all services
docker-compose up -d

# 3. Wait for services to be ready (~30 seconds)
docker-compose logs -f app

# 4. Verify installation
curl http://localhost:8080/api/v1/health/status
```

### Option 2: Local Development

```bash
# 1. Start dependencies
docker-compose up -d postgres redis

# 2. Set environment variables
export DB_HOST=localhost
export DB_PORT=5433
export REDIS_HOST=localhost

# 3. Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### ✓ Verification Checklist

- [ ] Application starts successfully
- [ ] Health check responds: `curl http://localhost:8080/api/v1/health/status`
- [ ] Swagger UI accessible: http://localhost:8080/api/v1/swagger-ui.html
- [ ] Database connection established
- [ ] Redis connection established

---

## 🛠️ Prerequisites

> **💡 Pro Tip**: Click on each item to verify your installation!

<details>
<summary>☕ <strong>Java 17+</strong> - Required Runtime</summary>

```bash
# Check Java version
java --version

# Should show Java 17 or higher
# openjdk 17.0.7 2023-04-18
```

**Installation:**
- **macOS**: `brew install openjdk@17`
- **Windows**: [Download OpenJDK](https://adoptium.net/)
- **Linux**: `sudo apt install openjdk-17-jdk`

</details>

<details>
<summary>📦 <strong>Maven 3.9+</strong> - Build Tool</summary>

```bash
# Check Maven version
mvn --version

# Should show Maven 3.9.0 or higher
# Apache Maven 3.9.4
```

**Installation:**
- **macOS**: `brew install maven`
- **Windows**: [Download Maven](https://maven.apache.org/download.cgi)
- **Linux**: `sudo apt install maven`

**Alternative**: Use included Maven wrapper: `./mvnw` (no installation needed)

</details>

<details>
<summary>🐳 <strong>Docker & Docker Compose</strong> - Containerization</summary>

```bash
# Check Docker version
docker --version
docker-compose --version

# Should show:
# Docker version 24.0.0 or higher
# Docker Compose version 2.20.0 or higher
```

**Installation:**
- [Download Docker Desktop](https://www.docker.com/products/docker-desktop)
- Docker Compose is included with Docker Desktop

</details>

<details>
<summary>🐘 <strong>PostgreSQL 15+</strong> - Primary Database</summary>

```bash
# Check PostgreSQL version (if installed locally)
psql --version

# Should show PostgreSQL 15.0 or higher
```

**Options:**
1. **Docker** (Recommended): Included in `docker-compose.yml`
2. **Local Install**: [Download PostgreSQL](https://www.postgresql.org/download/)
3. **Cloud**: AWS RDS, Google Cloud SQL, etc.

</details>

<details>
<summary>🔴 <strong>Redis 7.0+</strong> - Cache & Sessions</summary>

```bash
# Check Redis version (if installed locally)
redis-server --version

# Should show Redis 7.0.0 or higher
```

**Options:**
1. **Docker** (Recommended): Included in `docker-compose.yml`
2. **Local Install**: 
   - **macOS**: `brew install redis`
   - **Windows**: [Download Redis](https://redis.io/download)
   - **Linux**: `sudo apt install redis-server`

</details>

<details>
<summary>🌐 <strong>Optional: ngrok</strong> - Webhook Testing</summary>

```bash
# Check ngrok installation
ngrok version

# Should show ngrok version 3.0.0 or higher
```

**Installation:**
- **macOS**: `brew install ngrok/ngrok/ngrok`
- **Windows/Linux**: [Download from ngrok.com](https://ngrok.com/download)

**Purpose**: Required only for testing Authorize.Net webhooks locally

</details>

### ✓ System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **RAM** | 4GB | 8GB+ |
| **CPU** | 2 cores | 4+ cores |
| **Disk** | 2GB free | 10GB+ free |
| **Network** | Internet connection | Stable broadband |

### 🚨 Port Requirements

| Port | Service | Configurable |
|------|---------|-------------|
| **8080** | Application | ✓ |
| **5433** | PostgreSQL | ✓ |
| **6379** | Redis | ✓ |
| **3000** | Grafana | ✓ |
| **9090** | Prometheus | ✓ |

---

## � Installation

### Step 1: Clone Repository

```bash
# Clone the repository
git clone https://github.com/your-org/payment-gateway.git
cd payment-gateway

# Verify directory structure
ls -la
```

### Step 2: Environment Configuration

<details>
<summary>🔧 <strong>Environment Setup</strong> - Click to expand</summary>

#### Create Environment File

```bash
# Create environment file from template
cp env.template .env

# Edit with your preferred editor
nano .env  # or code .env, vim .env, etc.
```

#### Environment Variables

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5433
DB_NAME=payment_gateway
DB_USERNAME=payment_user
DB_PASSWORD=postgres_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=  # Leave empty for local development

# Authorize.Net Sandbox Credentials
AUTHNET_API_LOGIN_ID=your-sandbox-api-login-id
AUTHNET_TRANSACTION_KEY=your-sandbox-transaction-key
AUTHNET_ENVIRONMENT=SANDBOX  # or PRODUCTION

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production
JWT_EXPIRATION=86400  # 24 hours in seconds

# Webhook Configuration
WEBHOOK_SIGNATURE_SECRET=your-webhook-signature-secret
NGROK_PUBLIC_URL=https://your-ngrok-url.ngrok-free.app

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

# Monitoring Configuration
MANAGEMENT_ENDPOINTS_ENABLED=true
METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

#### 🔒 Security Notes

> **⚠️ Warning**: Never commit `.env` files to version control!

- Add `.env` to your `.gitignore`
- Use different secrets for each environment
- Rotate JWT secrets regularly in production
- Use strong, unique passwords for databases

</details>

### Step 3: Dependency Installation

```bash
# Download and install dependencies
./mvnw clean install

# Or if you have Maven installed globally
mvn clean install
```

### Step 4: Verify Installation

```bash
# Check Maven can build the project
./mvnw clean compile

# Run quick tests to verify setup
./mvnw test -Dtest="*UnitTest"
```

---
## 🗄️ Database Setup

### Option 1: Docker (Recommended)

<details>
<summary>🐳 <strong>Using Docker Compose</strong> - Easiest Setup</summary>

#### Start Database Services

```bash
# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Verify containers are running
docker-compose ps

# Expected output:
# NAME                    COMMAND                  SERVICE    STATUS    PORTS
# payment-gateway-postgres   "docker-entrypoint.s…"   postgres   running   0.0.0.0:5433->5432/tcp
# payment-gateway-redis      "docker-entrypoint.s…"   redis      running   0.0.0.0:6379->6379/tcp
```

#### Health Checks

```bash
# Check PostgreSQL readiness
docker exec payment-gateway-postgres pg_isready -U payment_user
# Expected: accepting connections

# Check Redis connectivity
docker exec payment-gateway-redis redis-cli ping
# Expected: PONG

# View database logs (if needed)
docker-compose logs postgres
docker-compose logs redis
```

</details>

### Option 2: Local Installation

<details>
<summary>💻 <strong>Local Database Setup</strong> - Advanced Users</summary>

#### PostgreSQL Setup

```bash
# Install PostgreSQL (varies by OS)
# macOS: brew install postgresql@15
# Ubuntu: sudo apt install postgresql-15
# Windows: Download from postgresql.org

# Start PostgreSQL service
# macOS: brew services start postgresql@15
# Linux: sudo systemctl start postgresql
# Windows: Start via Services manager

# Create database and user
sudo -u postgres psql
```

```sql
-- Create database and user
CREATE DATABASE payment_gateway;
CREATE USER payment_user WITH PASSWORD 'postgres_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE payment_gateway TO payment_user;
GRANT ALL ON SCHEMA public TO payment_user;

-- Exit psql
\q
```

#### Redis Setup

```bash
# Install Redis (varies by OS)
# macOS: brew install redis
# Ubuntu: sudo apt install redis-server
# Windows: Download from redis.io

# Start Redis service
# macOS: brew services start redis
# Linux: sudo systemctl start redis
# Windows: Start redis-server.exe

# Test Redis connection
redis-cli ping
# Expected: PONG
```

</details>

### Database Schema Migration

```bash
# Run database migrations using Flyway
./mvnw flyway:migrate

# Or with specific profile
./mvnw flyway:migrate -Dspring.profiles.active=dev

# Check migration status
./mvnw flyway:info
```

### Database Verification

<details>
<summary>🔍 <strong>Verify Database Setup</strong> - Click to expand</summary>

#### Connection Test

```bash
# Test PostgreSQL connection
psql -h localhost -p 5433 -U payment_user -d payment_gateway

# List tables (should see Flyway schema_version and application tables)
\dt

# Expected tables:
# public | payments              | table | payment_user
# public | subscriptions         | table | payment_user
# public | users                 | table | payment_user
# public | webhook_events        | table | payment_user
# public | schema_version        | table | payment_user

# Exit psql
\q
```

#### Application Connection Test

```bash
# Test database connectivity through application
curl http://localhost:8080/api/v1/health/detailed

# Check database component status
curl http://localhost:8080/api/v1/health/detailed | jq '.components.database'

# Expected output:
# {
#   "status": "UP",
#   "details": {
#     "database": "PostgreSQL",
#     "validationQuery": "isValid()"
#   }
# }
```

</details>

### 🚑 Database Troubleshooting

<details>
<summary>🚫 <strong>Common Database Issues</strong> - Click to expand</summary>

#### Connection Refused

```bash
# Check if PostgreSQL is running
docker-compose ps postgres
# or
sudo systemctl status postgresql

# Check port availability
netstat -tlnp | grep 5433
lsof -i :5433
```

#### Authentication Failed

```bash
# Verify credentials in .env file
cat .env | grep DB_

# Test connection manually
psql "postgresql://payment_user:postgres_password@localhost:5433/payment_gateway"
```

#### Migration Failures

```bash
# Check migration status
./mvnw flyway:info

# Clean and re-run migrations (⚠️ destroys data)
./mvnw flyway:clean
./mvnw flyway:migrate

# View migration history
./mvnw flyway:history
```

#### Redis Connection Issues

```bash
# Test Redis connectivity
redis-cli -h localhost -p 6379 ping

# Check Redis info
redis-cli info server

# Monitor Redis commands (debugging)
redis-cli monitor
```

</details>

---

## 🚀 Running the Application

### 🎆 Method 1: Docker Compose (Full Stack)

> **🔥 Recommended for beginners and complete local development**

<details>
<summary>🐳 <strong>Full Docker Setup</strong> - Everything in containers</summary>

```bash
# Build and start all services (app, database, redis, monitoring)
docker-compose up --build

# Or run in background
docker-compose up -d --build

# Watch logs in real-time
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

#### Service Health Checks

```bash
# Check all services status
docker-compose ps

# Quick health verification
curl http://localhost:8080/api/v1/health/status

# Detailed service health
curl http://localhost:8080/api/v1/health/detailed | jq
```

</details>

### ⚙️ Method 2: Local Development (Recommended)

> **💻 Best for active development and debugging**

<details>
<summary>🔧 <strong>Local Development Setup</strong> - App local, services in Docker</summary>

#### Step 1: Start Infrastructure

```bash
# Start only PostgreSQL and Redis
docker-compose up -d postgres redis

# Optional: Start monitoring stack
docker-compose up -d prometheus grafana
```

#### Step 2: Run Application Locally

```bash
# Method A: Using Maven wrapper (recommended)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Method B: Using global Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Method C: With debug logging
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dlogging.level.com.talentica.paymentgateway=DEBUG

# Method D: With JVM debugging enabled
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

#### Step 3: Verify Local Setup

```bash
# Health check
curl http://localhost:8080/api/v1/health/status

# Database connectivity
curl http://localhost:8080/api/v1/health/detailed | jq '.components.database'

# Redis connectivity
curl http://localhost:8080/api/v1/health/detailed | jq '.components.redis'
```

</details>

### 📦 Method 3: JAR Execution

> **🎢 Best for production-like testing**

<details>
<summary>🎯 <strong>Standalone JAR</strong> - Self-contained execution</summary>

#### Build JAR

```bash
# Create executable JAR
./mvnw clean package -DskipTests

# Or with tests
./mvnw clean package

# Check JAR file
ls -la target/payment-gateway-*.jar
```

#### Run JAR

```bash
# Basic execution
java -jar target/payment-gateway-1.0.0.jar

# With specific profile
java -jar target/payment-gateway-1.0.0.jar --spring.profiles.active=dev

# With environment variables
DB_HOST=localhost DB_PORT=5433 \
java -jar target/payment-gateway-1.0.0.jar --spring.profiles.active=dev

# With custom JVM settings
java -Xmx512m -Xms256m \
  -jar target/payment-gateway-1.0.0.jar --spring.profiles.active=prod
```

</details>

### 🌍 Method 4: Development with Hot Reload

<details>
<summary>🔄 <strong>Hot Reload Setup</strong> - For rapid development</summary>

#### Enable Spring Boot DevTools

```bash
# Run with DevTools for hot reload
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# In another terminal, make changes to Java files
# The application will automatically restart
```

#### IDE Configuration

**IntelliJ IDEA:**
1. Enable "Build project automatically"
2. Enable "Allow auto-make to start even if developed application is currently running"
3. Run configuration with "Update classes and resources"

**VS Code:**
1. Install "Spring Boot Extension Pack"
2. Use "Spring Boot Dashboard" to manage applications
3. Enable auto-restart in settings

</details>

### ✓ Application Startup Verification

```bash
# Basic health check
curl http://localhost:8080/api/v1/health/status
# Expected: {"status":"UP"}

# Detailed health information
curl http://localhost:8080/api/v1/health/detailed | jq

# Check application info
curl http://localhost:8080/api/v1/actuator/info | jq

# Verify database connection
curl http://localhost:8080/api/v1/actuator/health | jq '.components.db'

# Check all actuator endpoints
curl http://localhost:8080/api/v1/actuator | jq '."_links"'
```

### 📁 Application Logs

<details>
<summary>📄 <strong>Log Management</strong> - Monitoring and debugging</summary>

#### View Logs

```bash
# Docker logs
docker-compose logs -f app

# Local application logs (if configured)
tail -f logs/payment-gateway.log

# Structured log viewing with jq
docker-compose logs app | jq -r '. | select(.level=="ERROR")'
```

#### Log Levels

```yaml
# application-dev.yml
logging:
  level:
    com.talentica.paymentgateway: DEBUG
    org.springframework.security: INFO
    net.authorize: DEBUG
    org.springframework.web: DEBUG
```

#### Log Correlation

Each request gets a unique correlation ID for tracing:

```json
{
  "timestamp": "2025-01-05T10:30:00.000Z",
  "level": "INFO",
  "correlationId": "abc123-def456-ghi789",
  "logger": "com.talentica.paymentgateway.service.PaymentService",
  "message": "Processing payment transaction",
  "transactionId": "txn_123456"
}
```

</details>

---
## ⚙️ Background Workers

> **💡 Note**: Background jobs run within the main application process - no separate worker services required!

### 🔄 Scheduled Tasks

<details>
<summary>📅 <strong>Recurring Job Types</strong> - Click to expand</summary>

#### 1. Subscription Billing Engine

```java
@Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
public void processSubscriptionBilling() {
    // Process due subscription payments
    // Handles retries for failed payments
    // Sends notifications to customers
}
```

**Features:**
- 📅 Daily billing cycle processing
- 🔄 Automatic retry logic for failures
- 📧 Email notifications for payment issues
- 📊 Analytics tracking for billing metrics

#### 2. Transaction Cleanup

```java
@Scheduled(fixedDelay = 3600000) // Every hour
public void cleanupExpiredTransactions() {
    // Remove expired pending transactions
    // Archive completed transactions
    // Update transaction statuses
}
```

#### 3. Health Monitoring

```java
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void monitorSystemHealth() {
    // Check external service connectivity
    // Monitor database performance
    // Update health metrics
}
```

</details>

### 📨 Async Webhook Processing

<details>
<summary>🔗 <strong>Webhook Event Handling</strong> - Click to expand</summary>

#### Event Processing Pipeline

```mermaid
flowchart LR
    A[Webhook Received] --> B[Signature Validation]
    B --> C[Event Queuing]
    C --> D[Async Processing]
    D --> E[Business Logic]
    E --> F[Database Update]
    F --> G[Notification Sent]
    D --> H[Retry on Failure]
    H --> D
```

#### Webhook Configuration

```yaml
# application.yml
app:
  webhook:
    processing:
      async: true
      max-retries: 3
      retry-delay: 5000ms
      thread-pool-size: 10
```

#### Supported Webhook Events

| Event Type | Description | Processing |
|------------|-------------|------------|
| `payment.succeeded` | Successful payment | Update transaction status |
| `payment.failed` | Failed payment | Retry logic + notification |
| `subscription.created` | New subscription | Setup billing schedule |
| `subscription.cancelled` | Cancelled subscription | Stop billing + cleanup |
| `chargeback.created` | Chargeback initiated | Fraud detection + alert |

</details>

### 📈 Monitoring Background Jobs

<details>
<summary>🔍 <strong>Job Monitoring & Metrics</strong> - Click to expand</summary>

#### Check Job Status

```bash
# View scheduled task metrics
curl http://localhost:8080/api/v1/actuator/scheduledtasks | jq

# Check async task executor status
curl http://localhost:8080/api/v1/actuator/metrics/executor.active | jq

# Monitor webhook processing queue
curl http://localhost:8080/api/v1/actuator/metrics/webhook.queue.size | jq
```

#### Custom Metrics

```java
@Component
public class JobMetrics {
    private final Counter billingJobCounter;
    private final Timer billingJobTimer;
    
    @EventListener
    public void onBillingJobComplete(BillingJobCompleteEvent event) {
        billingJobCounter.increment();
        billingJobTimer.record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
}
```

#### Grafana Dashboard Queries

```promql
# Billing job success rate
rate(billing_job_success_total[5m]) / rate(billing_job_total[5m]) * 100

# Webhook processing latency
histogram_quantile(0.95, rate(webhook_processing_duration_seconds_bucket[5m]))

# Active background threads
executor_active_threads{job="payment-gateway"}
```

</details>

### 🚫 Troubleshooting Background Jobs

<details>
<summary>🚑 <strong>Common Issues & Solutions</strong> - Click to expand</summary>

#### Job Not Running

```bash
# Check if scheduling is enabled
curl http://localhost:8080/api/v1/actuator/configprops | grep scheduling

# Verify cron expressions
curl http://localhost:8080/api/v1/actuator/scheduledtasks | jq '.cron'
```

#### Database Lock Issues

```sql
-- Check for locked transactions
SELECT * FROM pg_stat_activity WHERE state = 'active' AND query LIKE '%subscription%';

-- Kill long-running queries (if needed)
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid = <process_id>;
```

#### Memory Issues

```bash
# Monitor JVM memory usage
curl http://localhost:8080/api/v1/actuator/metrics/jvm.memory.used | jq

# Check garbage collection metrics
curl http://localhost:8080/api/v1/actuator/metrics/jvm.gc.pause | jq
```

#### Thread Pool Exhaustion

```yaml
# Increase thread pool size in application.yml
spring:
  task:
    scheduling:
      pool:
        size: 20  # Increase from default 1
    execution:
      pool:
        core-size: 8
        max-size: 16
```

</details>

### 🔧 Configuration Options

```yaml
# application.yml - Background job configuration
app:
  scheduling:
    enabled: true
    billing:
      cron: "0 0 2 * * ?"  # Daily at 2 AM
      timezone: "America/New_York"
    cleanup:
      enabled: true
      fixed-delay: 3600000  # 1 hour
    
  async:
    webhook:
      enabled: true
      core-pool-size: 5
      max-pool-size: 20
      queue-capacity: 100
      thread-name-prefix: "webhook-"
    
  retry:
    max-attempts: 3
    backoff-delay: 5000ms
    multiplier: 2.0
```

---

## 📊 Monitoring

### 🏥 Health Checks

<details>
<summary>❤️ <strong>Health Check Endpoints</strong> - Click to expand</summary>

#### Available Health Endpoints

| Endpoint | Purpose | Response | Use Case |
|----------|---------|----------|----------|
| `/api/v1/health/status` | Basic health | `{"status":"UP"}` | Quick health check |
| `/api/v1/health/detailed` | Detailed health | Full component status | Debugging |
| `/api/v1/health/ready` | Readiness probe | Ready for traffic | Kubernetes |
| `/api/v1/health/live` | Liveness probe | Application alive | Kubernetes |

#### Health Check Commands

```bash
# Quick health check
curl http://localhost:8080/api/v1/health/status

# Detailed health with all components
curl http://localhost:8080/api/v1/health/detailed | jq

# Check specific component health
curl http://localhost:8080/api/v1/health/detailed | jq '.components.database'

# Monitor health continuously
watch -n 5 'curl -s http://localhost:8080/api/v1/health/status'
```

#### Custom Health Indicators

```java
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        if (isAuthorizeNetReachable()) {
            return Health.up()
                .withDetail("authorize-net", "Available")
                .withDetail("response-time", "250ms")
                .build();
        }
        return Health.down()
            .withDetail("authorize-net", "Unavailable")
            .build();
    }
}
```

</details>

### 📈 Metrics and Monitoring

<details>
<summary>📊 <strong>Monitoring Stack</strong> - Click to expand</summary>

#### Monitoring URLs

| Service | URL | Credentials | Purpose |
|---------|-----|-------------|---------|
| **Prometheus** | http://localhost:9090 | None | Metrics collection |
| **Grafana** | http://localhost:3000 | admin/admin | Dashboards |
| **Zipkin** | http://localhost:9411 | None | Distributed tracing |
| **Application Metrics** | http://localhost:8080/api/v1/actuator/prometheus | None | Raw metrics |

#### Key Metrics to Monitor

```bash
# Application metrics
curl http://localhost:8080/api/v1/actuator/metrics/payment.transactions.total

# JVM metrics
curl http://localhost:8080/api/v1/actuator/metrics/jvm.memory.used

# HTTP request metrics
curl http://localhost:8080/api/v1/actuator/metrics/http.server.requests

# Database connection pool
curl http://localhost:8080/api/v1/actuator/metrics/hikaricp.connections.active
```

#### Custom Business Metrics

```java
@Component
public class PaymentMetrics {
    private final Counter paymentCounter;
    private final Timer paymentTimer;
    
    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.paymentCounter = Counter.builder("payment.transactions")
            .description("Total payment transactions")
            .tag("type", "purchase")
            .register(meterRegistry);
            
        this.paymentTimer = Timer.builder("payment.processing.time")
            .description("Payment processing time")
            .register(meterRegistry);
    }
}
```

</details>

### 📋 Structured Logging

<details>
<summary>📝 <strong>Log Management</strong> - Click to expand</summary>

#### Log Format

```json
{
  "timestamp": "2025-01-05T10:30:00.000Z",
  "level": "INFO",
  "correlationId": "abc123-def456-ghi789",
  "logger": "com.talentica.paymentgateway.service.PaymentService",
  "message": "Processing payment transaction",
  "transactionId": "txn_123456",
  "userId": "user_789",
  "amount": 100.00,
  "currency": "USD"
}
```

#### Log Levels Configuration

```yaml
# application.yml
logging:
  level:
    com.talentica.paymentgateway: INFO
    org.springframework.security: WARN
    net.authorize: DEBUG
    org.springframework.web: INFO
    
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
```

#### Log Analysis Commands

```bash
# View application logs
docker-compose logs -f app

# Filter by log level
docker-compose logs app | grep "ERROR"

# Search for specific correlation ID
docker-compose logs app | grep "abc123-def456-ghi789"

# Monitor payment transactions
docker-compose logs app | grep "Processing payment"

# JSON log parsing
docker-compose logs app --tail=100 | jq 'select(.level=="ERROR")'
```

#### Log Correlation

Each request gets a unique correlation ID that flows through:
- HTTP requests/responses
- Database operations  
- External API calls
- Async message processing
- Background job execution

</details>

### 🚨 Alerting and Notifications

<details>
<summary>🔔 <strong>Alert Configuration</strong> - Click to expand</summary>

#### Grafana Alerts

```yaml
# grafana/provisioning/alerting/alerts.yml
groups:
  - name: payment-gateway-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: High error rate detected
          
      - alert: DatabaseConnections
        expr: hikaricp_connections_active > 8
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: High database connection usage
```

#### Notification Channels

- **Slack**: Payment processing alerts
- **Email**: Critical system failures  
- **PagerDuty**: Production outages
- **Webhook**: Custom integrations

</details>

---

## 🧪 Testing

### 🏃‍♂️ Quick Test Commands

```bash
# Run all tests
./mvnw test

# Run with coverage report
./mvnw test jacoco:report

# Run specific test class
./mvnw test -Dtest=PaymentServiceTest

# Run tests matching pattern
./mvnw test -Dtest="*Integration*Test"

# Skip tests during build
./mvnw package -DskipTests
```

### 📈 Test Categories

<details>
<summary>🧩 <strong>Unit Tests</strong> - Fast, isolated tests</summary>

#### Service Layer Tests

```bash
# Test payment processing logic
./mvnw test -Dtest="*ServiceTest"

# Test with mockito verification
./mvnw test -Dtest=PaymentServiceUnitTest -Dmaven.test.debug=true
```

#### Controller Layer Tests

```bash
# Test REST endpoints with @WebMvcTest
./mvnw test -Dtest="*ControllerTest"

# Test with custom security context
./mvnw test -Dtest=PaymentControllerUnitTest
```

#### Repository Layer Tests

```bash
# Test JPA queries with @DataJpaTest
./mvnw test -Dtest="*RepositoryTest"

# Test with H2 in-memory database
./mvnw test -Dtest=PaymentRepositoryTest -Dspring.profiles.active=test
```

</details>

<details>
<summary>🎭 <strong>Integration Tests</strong> - Full application context</summary>

#### Database Integration

```bash
# Run with PostgreSQL testcontainer
./mvnw test -Dtest="*IntegrationTest" -Dspring.profiles.active=integration

# Test database migrations
./mvnw test -Dtest=DatabaseMigrationTest
```

#### External Service Integration

```bash
# Test Authorize.Net integration (requires sandbox credentials)
export AUTHNET_API_LOGIN_ID=sandbox_login
export AUTHNET_TRANSACTION_KEY=sandbox_key
./mvnw test -Dtest=AuthorizeNetIntegrationTest

# Test webhook processing
./mvnw test -Dtest=WebhookIntegrationTest
```

#### End-to-End Tests

```bash
# Full application tests
./mvnw verify

# Test with TestRestTemplate
./mvnw test -Dtest=PaymentGatewayApplicationTests
```

</details>

<details>
<summary>🔒 <strong>Security Tests</strong> - Authentication & authorization</summary>

```bash
# Test JWT authentication
./mvnw test -Dtest="*SecurityTest"

# Test method-level security
./mvnw test -Dtest=SecurityConfigTest

# Test rate limiting
./mvnw test -Dtest=RateLimitingTest
```

</details>

### 📊 Test Coverage

<details>
<summary>📈 <strong>Coverage Reports</strong> - Click to expand</summary>

#### Generate Coverage Report

```bash
# Run tests with coverage
./mvnw clean test jacoco:report

# View HTML report
open target/site/jacoco/index.html

# View coverage summary
./mvnw jacoco:report | grep "Instructions covered"
```

#### Coverage Requirements

- **Overall Coverage**: ≥80%
- **Service Layer**: ≥90%
- **Controller Layer**: ≥85%
- **Repository Layer**: ≥75%

#### Coverage Configuration

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

</details>

### 🐞 Test Data Management

<details>
<summary>📋 <strong>Test Data Setup</strong> - Click to expand</summary>

#### Test Profiles

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    
  flyway:
    enabled: false  # Disable for unit tests

app:
  authorize-net:
    environment: SANDBOX
    api-login-id: test_login
    transaction-key: test_key
```

#### Test Fixtures

```java
@TestConfiguration
public class TestDataConfig {
    
    @Bean
    @Primary
    public PaymentTestDataFactory paymentTestDataFactory() {
        return new PaymentTestDataFactory();
    }
    
    @PostConstruct
    public void setupTestData() {
        // Initialize test data
    }
}
```

#### Database Cleanup

```java
@Transactional
@Rollback
class PaymentServiceIntegrationTest {
    
    @BeforeEach
    void setUp() {
        testDataFactory.createTestPayment();
    }
    
    @AfterEach
    void tearDown() {
        testDataFactory.cleanupTestData();
    }
}
```

</details>

### 🛠️ Test Debugging

<details>
<summary>🔍 <strong>Debug Test Failures</strong> - Click to expand</summary>

#### Debug Single Test

```bash
# Run test with debug output
./mvnw test -Dtest=PaymentServiceTest -Dmaven.test.debug=true

# Run with specific logging level
./mvnw test -Dtest=PaymentServiceTest \
  -Dlogging.level.com.talentica.paymentgateway=DEBUG
```

#### Test Report Analysis

```bash
# View detailed test reports
open target/surefire-reports/index.html

# Check specific test output
cat target/surefire-reports/com.talentica.paymentgateway.service.PaymentServiceTest.txt

# View test execution times
grep "Time elapsed" target/surefire-reports/*.txt
```

#### Common Test Issues

| Issue | Solution |
|-------|----------|
| **ApplicationContext fails to load** | Check test configuration, verify mock beans |
| **Database connection errors** | Ensure test database is available, check credentials |
| **Flaky tests** | Add proper wait conditions, isolate test data |
| **Memory issues** | Increase heap size: `-Xmx1024m` |
| **Timeout failures** | Increase timeout values in test configuration |

</details>

### 🏃‍♀️ Continuous Testing

```bash
# Watch mode for development
./mvnw test -Dspring-boot.run.fork=false

# Run tests on file change (with entr)
find src -name "*.java" | entr ./mvnw test

# Maven surefire plugin with parallel execution
./mvnw test -DforkCount=4 -DreuseForks=true
```

---

## 🔧 Configuration

### 🌍 Environment Profiles

<details>
<summary>🛠️ <strong>Profile Management</strong> - Click to expand</summary>

| Profile | Purpose | Database | Features |
|---------|---------|----------|----------|
| **dev** | Local development | H2/PostgreSQL | Debug logging, hot reload |
| **test** | Unit/Integration tests | H2 in-memory | Fast startup, isolated |
| **staging** | Pre-production testing | PostgreSQL | Production-like config |
| **prod** | Production deployment | PostgreSQL | Optimized performance |

#### Profile Activation

```bash
# Environment variable
export SPRING_PROFILES_ACTIVE=dev

# Command line
java -jar app.jar --spring.profiles.active=prod

# Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging

# Docker
docker run -e SPRING_PROFILES_ACTIVE=prod payment-gateway
```

</details>

### ⚙️ Key Configuration Properties

<details>
<summary>📝 <strong>Application Settings</strong> - Click to expand</summary>

#### Core Application

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `server.port` | Application port | 8080 | ❌ |
| `spring.application.name` | Service name | payment-gateway | ❌ |
| `spring.profiles.active` | Active profile | dev | ✅ |

#### Database Configuration

| Property | Description | Example | Required |
|----------|-------------|---------|----------|
| `spring.datasource.url` | Database URL | `jdbc:postgresql://localhost:5433/payment_gateway` | ✅ |
| `spring.datasource.username` | Database user | `payment_user` | ✅ |
| `spring.datasource.password` | Database password | `postgres_password` | ✅ |
| `spring.jpa.hibernate.ddl-auto` | Schema management | `validate` | ❌ |

#### Redis Configuration

| Property | Description | Example | Required |
|----------|-------------|---------|----------|
| `spring.data.redis.host` | Redis host | `localhost` | ✅ |
| `spring.data.redis.port` | Redis port | `6379` | ✅ |
| `spring.data.redis.password` | Redis password | `redis_pass` | ❌ |
| `spring.data.redis.timeout` | Connection timeout | `2000ms` | ❌ |

#### Authorize.Net Settings

| Property | Description | Example | Required |
|----------|-------------|---------|----------|
| `app.authorize-net.api-login-id` | API Login ID | `sandbox_login` | ✅ |
| `app.authorize-net.transaction-key` | Transaction Key | `sandbox_key` | ✅ |
| `app.authorize-net.environment` | Environment | `SANDBOX`/`PRODUCTION` | ✅ |
| `app.authorize-net.timeout` | Request timeout | `30s` | ❌ |

#### Security Configuration

| Property | Description | Example | Required |
|----------|-------------|---------|----------|
| `app.jwt.secret` | JWT signing key | `your-secret-key` | ✅ |
| `app.jwt.expiration` | Token expiry (seconds) | `86400` | ❌ |
| `app.rate-limit.default-limit` | Requests per hour | `1000` | ❌ |
| `app.cors.allowed-origins` | CORS origins | `http://localhost:3000` | ❌ |

</details>

### 📋 Configuration Files

<details>
<summary>📁 <strong>Configuration File Structure</strong> - Click to expand</summary>

```
src/main/resources/
├── application.yml              # Base configuration
├── application-dev.yml          # Development overrides
├── application-test.yml         # Test configuration
├── application-staging.yml      # Staging configuration
├── application-prod.yml         # Production configuration
└── logback-spring.xml           # Logging configuration
```

#### Example: application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/payment_gateway
    username: payment_user
    password: postgres_password
  
  data:
    redis:
      host: localhost
      port: 6379
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate

app:
  authorize-net:
    api-login-id: ${AUTHNET_API_LOGIN_ID:sandbox_login}
    transaction-key: ${AUTHNET_TRANSACTION_KEY:sandbox_key}
    environment: SANDBOX
  
  jwt:
    secret: ${JWT_SECRET:dev-secret-key}
    expiration: 86400

logging:
  level:
    com.talentica.paymentgateway: DEBUG
    org.springframework.web: DEBUG
```

</details>

### 📊 Actuator Configuration

<details>
<summary>📈 <strong>Monitoring Endpoints</strong> - Click to expand</summary>

```yaml
# Actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,configprops
      base-path: /api/v1/actuator
  
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
    
    metrics:
      enabled: true
  
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: payment-gateway
      environment: ${spring.profiles.active}
```

#### Available Endpoints

| Endpoint | Purpose | URL |
|----------|---------|-----|
| Health | Service health | `/actuator/health` |
| Info | Application info | `/actuator/info` |
| Metrics | Custom metrics | `/actuator/metrics` |
| Prometheus | Metrics export | `/actuator/prometheus` |
| Environment | Config properties | `/actuator/env` |
| Config Props | Configuration | `/actuator/configprops` |

</details>

## 🔐 Security

### Authentication

The application uses JWT-based authentication:

1. **Obtain JWT Token**: POST `/api/v1/auth/login`
2. **Use Token**: Include `Authorization: Bearer <token>` header
3. **Refresh Token**: POST `/api/v1/auth/refresh`

### API Key Authentication

For service-to-service communication:

1. **Generate API Key**: Through admin interface
2. **Use API Key**: Include `X-API-Key: <key>` header

### Security Features

- JWT token authentication with RS256 algorithm
- API key-based authentication for services
- Rate limiting with Redis backend
- CORS configuration for cross-origin requests
- Input validation and sanitization
- SQL injection prevention
- Security headers (HSTS, CSP, etc.)

## 🚀 Deployment

### Docker Deployment

```bash
# Build Docker image
docker build -t payment-gateway:latest .

# Run container
docker run -d \
  --name payment-gateway \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  -e DB_USERNAME=your-db-user \
  -e DB_PASSWORD=your-db-password \
  payment-gateway:latest
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-gateway
  template:
    metadata:
      labels:
        app: payment-gateway
    spec:
      containers:
      - name: payment-gateway
        image: payment-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        livenessProbe:
          httpGet:
            path: /api/v1/health/live
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /api/v1/health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

## 📚 API Documentation

### Interactive Documentation

Visit http://localhost:8080/api/v1/swagger-ui.html for interactive API documentation.

### Core Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/payments/purchase` | POST | Process one-time payment |
| `/api/v1/payments/authorize` | POST | Authorize payment |
| `/api/v1/payments/capture` | POST | Capture authorized payment |
| `/api/v1/payments/void` | POST | Void authorization |
| `/api/v1/payments/refund` | POST | Process refund |
| `/api/v1/subscriptions` | POST | Create subscription |
| `/api/v1/webhooks` | POST | Webhook endpoint |

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   ```bash
   # Check database is running
   docker-compose ps postgres
   
   # Check database logs
   docker-compose logs postgres
   ```

2. **Redis Connection Failed**
   ```bash
   # Check Redis is running
   docker-compose ps redis
   
   # Test Redis connection
   redis-cli ping
   ```

3. **Authorize.Net API Errors**
   - Verify sandbox credentials in `.env` file
   - Check Authorize.Net sandbox status
   - Review application logs for detailed error messages

4. **Port Already in Use**
   ```bash
   # Find process using port 8080
   lsof -i :8080
   
   # Kill process
   kill -9 <PID>
   ```

5. **Webhook Testing with ngrok**
   ```bash
   # Install ngrok
   brew install ngrok/ngrok/ngrok
   
   # Start tunnel to your local app
   ngrok http 8080
   
   # Use the HTTPS URL for webhooks:
   # https://abc123.ngrok-free.app/api/v1/webhooks/authorize-net

   # dashboard
   # https://dashboard.ngrok.com/agents
   
   # Monitor requests at: http://localhost:4040
   ```

### Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.talentica.paymentgateway: DEBUG
    org.springframework.security: DEBUG
    net.authorize: DEBUG
```

## 🤝 Contributing

> **🌟 We welcome contributions! Follow these steps to get started.**

### 🚀 Quick Contribution Guide

<details>
<summary>📋 <strong>Step-by-Step Process</strong> - Click to expand</summary>

#### 1. Fork & Clone

```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/payment-gateway.git
cd payment-gateway

# Add upstream remote
git remote add upstream https://github.com/original-repo/payment-gateway.git
```

#### 2. Set Up Development Environment

```bash
# Install dependencies
./mvnw clean install

# Start development environment
docker-compose up -d postgres redis

# Verify setup
./mvnw test
```

#### 3. Create Feature Branch

```bash
# Create and switch to feature branch
git checkout -b feature/amazing-new-feature

# Or for bug fixes
git checkout -b fix/issue-123

# Or for documentation
git checkout -b docs/update-readme
```

#### 4. Make Your Changes

```bash
# Make your changes
code .

# Test your changes
./mvnw test

# Check code style
./mvnw checkstyle:check

# Run integration tests
./mvnw verify
```

#### 5. Commit Your Changes

```bash
# Stage changes
git add .

# Commit with conventional commit message
git commit -m "feat: add new payment method validation"

# Or for bug fixes
git commit -m "fix: resolve null pointer in payment processing"

# Or for documentation
git commit -m "docs: update API documentation"
```

#### 6. Push and Create PR

```bash
# Push to your fork
git push origin feature/amazing-new-feature

# Create Pull Request through GitHub UI
```

</details>

### 📝 Development Guidelines

<details>
<summary>📚 <strong>Best Practices</strong> - Click to expand</summary>

#### Code Standards

- **Java**: Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- **Spring Boot**: Follow [Spring Boot best practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- **Testing**: Maintain ≥80% test coverage
- **Documentation**: Update docs for new features
- **Security**: Never commit secrets or credentials

#### Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description

feat(payments): add support for recurring billing
fix(auth): resolve JWT token expiration issue
docs(readme): update installation instructions
test(service): add unit tests for payment validation
refactor(controller): simplify error handling
chore(deps): update Spring Boot to 3.2.5
```

#### Code Review Checklist

- [ ] Tests pass (`./mvnw test`)
- [ ] Code coverage maintained (`./mvnw jacoco:report`)
- [ ] Documentation updated
- [ ] No security vulnerabilities
- [ ] Performance impact considered
- [ ] Backward compatibility maintained

</details>

### 🧪 Testing Your Contribution

```bash
# Run all tests
./mvnw clean test

# Run with coverage
./mvnw clean test jacoco:report

# Check coverage report
open target/site/jacoco/index.html

# Run integration tests
./mvnw clean verify

# Test specific functionality
./mvnw test -Dtest="*PaymentService*Test"
```

### 🐛 Reporting Issues

<details>
<summary>🎯 <strong>Issue Template</strong> - Click to expand</summary>

When reporting issues, please include:

#### Bug Report Template

```markdown
## Bug Description
A clear and concise description of the bug.

## Steps to Reproduce
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

## Expected Behavior
What you expected to happen.

## Actual Behavior
What actually happened.

## Environment
- OS: [e.g., macOS, Windows, Linux]
- Java Version: [e.g., OpenJDK 17.0.7]
- Spring Boot Version: [e.g., 3.2.5]
- Browser: [e.g., Chrome 91.0] (if applicable)

## Additional Context
Add any other context about the problem here.

## Logs
```
Paste relevant log output here
```
```

#### Feature Request Template

```markdown
## Feature Description
A clear and concise description of what you want to happen.

## Use Case
Explain the use case and business value.

## Proposed Solution
Describe your proposed solution.

## Alternatives Considered
Other solutions you've considered.

## Additional Context
Any other context or screenshots.
```

</details>

---

## 📄 License

This project is licensed under the **MIT License**.

<details>
<summary>📝 <strong>License Details</strong> - Click to expand</summary>

```
MIT License

Copyright (c) 2025 Payment Gateway Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

See the [LICENSE](LICENSE) file for complete details.

</details>

---

## 📞 Support & Community

### 🆘 Getting Help

<details>
<summary>💬 <strong>Support Channels</strong> - Click to expand</summary>

| Channel | Purpose | Response Time |
|---------|---------|---------------|
| **GitHub Issues** | Bug reports, feature requests | 24-48 hours |
| **GitHub Discussions** | Questions, help, ideas | 24 hours |
| **Email** | Private support | 48 hours |
| **Documentation** | Self-service help | Immediate |

#### Contact Information

- **📧 Email**: support@talentica.com
- **📚 Documentation**: [Project Wiki](https://github.com/your-org/payment-gateway/wiki)
- **🐛 Bug Reports**: [GitHub Issues](https://github.com/your-org/payment-gateway/issues)
- **💬 Discussions**: [GitHub Discussions](https://github.com/your-org/payment-gateway/discussions)

</details>

### 🌟 Community Guidelines

- Be respectful and inclusive
- Search existing issues before creating new ones
- Provide clear, reproducible examples
- Help others in the community
- Follow our [Code of Conduct](CODE_OF_CONDUCT.md)

---

## 🔄 Roadmap & Version History

### 🚀 Current Release: v1.0.0

<details>
<summary>📋 <strong>Release History</strong> - Click to expand</summary>

#### v1.0.0 (Current) - January 2025
- ✅ Core payment processing (Purchase, Authorize, Capture, Void, Refund)
- ✅ JWT authentication and API key management
- ✅ PostgreSQL and Redis integration
- ✅ Authorize.Net sandbox integration
- ✅ Comprehensive monitoring and health checks
- ✅ Docker containerization
- ✅ Rate limiting and security features
- ✅ Webhook processing with retry logic

#### v1.1.0 (Planned) - Q2 2025
- 🔄 Enhanced subscription billing engine
- 🔄 Advanced fraud detection
- 🔄 Multi-currency support
- 🔄 Payment method tokenization
- 🔄 Advanced analytics dashboard

#### v1.2.0 (Planned) - Q3 2025
- 🔄 Multi-gateway support (Stripe, PayPal)
- 🔄 Advanced webhook management UI
- 🔄 Real-time payment notifications
- 🔄 Enhanced reporting capabilities
- 🔄 Mobile SDK development

#### v2.0.0 (Planned) - Q4 2025
- 🔄 Microservices architecture
- 🔄 GraphQL API support
- 🔄 Advanced machine learning for fraud detection
- 🔄 International payment methods
- 🔄 Advanced compliance features (PCI-DSS Level 1)

</details>

### 🎯 Contributing to Roadmap

Have ideas for future features? 
- 💡 [Submit feature requests](https://github.com/your-org/payment-gateway/issues/new?template=feature_request.md)
- 🗳️ Vote on existing proposals
- 💬 Join our [planning discussions](https://github.com/your-org/payment-gateway/discussions)

---

<div align="center">

### 🏆 Built with ❤️ by the Payment Gateway Team

<p align="center">
  <img src="https://img.shields.io/github/contributors/your-org/payment-gateway?style=for-the-badge" alt="Contributors"/>
  <img src="https://img.shields.io/github/last-commit/your-org/payment-gateway?style=for-the-badge" alt="Last Commit"/>
  <img src="https://img.shields.io/github/commit-activity/m/your-org/payment-gateway?style=for-the-badge" alt="Commit Activity"/>
</p>

**⭐ If this project helped you, please consider giving it a star! ⭐**

---

### 🔗 Quick Links

| [📖 Documentation](wiki-url) | [🐛 Report Bug](issues-url) | [💡 Request Feature](issues-url) | [💬 Discussions](discussions-url) |
|---|---|---|---|

---

*Last updated: January 5, 2025*

</div>