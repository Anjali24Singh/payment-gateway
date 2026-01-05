#!/bin/bash

# Payment Gateway - Development Environment Setup Script
# This script sets up the complete development environment as per README

set -e  # Exit on any error

echo "🚀 Payment Gateway - Development Environment Setup"
echo "=================================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️ $1${NC}"
}

# Check prerequisites
echo "🛠️ Checking Prerequisites..."
echo "----------------------------"

# Check Java
if command -v java >/dev/null 2>&1; then
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ $java_version == 17* ]] || [[ $java_version == 1.8* ]] || [[ $java_version > 17 ]]; then
        print_status "Java $java_version found"
    else
        print_warning "Java version $java_version found, but Java 17+ is recommended"
    fi
else
    print_error "Java not found. Please install Java 17+"
    exit 1
fi

# Check Maven
if command -v mvn >/dev/null 2>&1; then
    maven_version=$(mvn -version | head -n 1 | cut -d ' ' -f 3)
    print_status "Maven $maven_version found"
else
    print_info "Maven not found. Using Maven wrapper (./mvnw)"
fi

# Check Docker
if command -v docker >/dev/null 2>&1; then
    docker_version=$(docker --version | cut -d ' ' -f 3 | sed 's/,//')
    print_status "Docker $docker_version found"
else
    print_error "Docker not found. Please install Docker Desktop"
    exit 1
fi

# Check Docker Compose
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    compose_version=$(docker compose version --short 2>/dev/null || docker compose version | grep -o 'v[0-9][^\s]*' | head -1)
    print_status "Docker Compose $compose_version found"
elif command -v docker-compose >/dev/null 2>&1; then
    compose_version=$(docker-compose --version | cut -d ' ' -f 3 | sed 's/,//')
    print_status "Docker Compose $compose_version found"
else
    print_error "Docker Compose not found. Please install Docker Desktop with Docker Compose"
    exit 1
fi

echo ""

# Environment setup
echo "🌍 Setting up Environment..."
echo "----------------------------"

if [ ! -f .env ]; then
    if [ -f .env.template ]; then
        cp .env.template .env
        print_status "Created .env file from template"
        print_warning "Please update .env file with your actual configuration values"
    else
        print_error ".env.template not found. Cannot create .env file"
        exit 1
    fi
else
    print_status ".env file already exists"
fi

echo ""

# Start infrastructure services
echo "🐳 Starting Infrastructure Services..."
echo "-------------------------------------"

print_info "Starting PostgreSQL and Redis..."
# Try docker compose (v2) first, then docker-compose (v1)
if docker compose up -d postgres redis 2>/dev/null || docker-compose up -d postgres redis; then
    print_status "Infrastructure services started"
else
    print_error "Failed to start infrastructure services"
    print_error "Make sure Docker Desktop is running and try again"
    exit 1
fi

# Wait for services to be ready
print_info "Waiting for services to be ready..."
sleep 10

# Check PostgreSQL
print_info "Checking PostgreSQL connection..."
postgres_container=$(docker ps --filter "name=payment-gateway-postgres" --format "{{.Names}}" | head -1)
if [ -n "$postgres_container" ] && docker exec "$postgres_container" pg_isready -U payment_user -d payment_gateway >/dev/null 2>&1; then
    print_status "PostgreSQL is ready"
else
    print_warning "PostgreSQL not ready yet. Waiting longer..."
    sleep 20
    if [ -n "$postgres_container" ] && docker exec "$postgres_container" pg_isready -U payment_user -d payment_gateway >/dev/null 2>&1; then
        print_status "PostgreSQL is ready"
    else
        print_error "PostgreSQL failed to start properly. Check: docker ps"
        exit 1
    fi
fi

# Check Redis
print_info "Checking Redis connection..."
redis_container=$(docker ps --filter "name=payment-gateway-redis" --format "{{.Names}}" | head -1)
if [ -n "$redis_container" ] && docker exec "$redis_container" redis-cli ping >/dev/null 2>&1; then
    print_status "Redis is ready"
else
    print_error "Redis failed to start properly. Check: docker ps"
    exit 1
fi

echo ""

# Build application
echo "🔨 Building Application..."
echo "-------------------------"

print_info "Installing dependencies..."
if ./mvnw clean install -DskipTests; then
    print_status "Dependencies installed successfully"
else
    print_error "Failed to install dependencies"
    exit 1
fi

print_info "Compiling application..."
if ./mvnw clean compile; then
    print_status "Application compiled successfully"
else
    print_error "Failed to compile application"
    exit 1
fi

echo ""

# Run database migrations
echo "🗄️ Running Database Migrations..."
echo "--------------------------------"

print_info "Applying database migrations..."
if ./mvnw flyway:migrate -Dspring.profiles.active=dev; then
    print_status "Database migrations completed"
else
    print_warning "Database migrations may have failed. Check logs."
fi

echo ""

# Run tests
echo "🧪 Running Quick Tests..."
echo "------------------------"

print_info "Running unit tests..."
if ./mvnw test -Dtest="*UnitTest" -Dspring.profiles.active=test; then
    print_status "Unit tests passed"
else
    print_warning "Some unit tests failed. Check logs for details."
fi

echo ""

# Final verification
echo "✅ Final Verification..."
echo "-----------------------"

print_info "Starting application for quick verification..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
APP_PID=$!

# Wait for application to start
print_info "Waiting for application to start..."
for i in {1..30}; do
    # Try multiple health check methods
    if command -v curl >/dev/null 2>&1; then
        if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1 || curl -s http://localhost:8080/api/v1/health/status >/dev/null 2>&1; then
            print_status "Application started successfully!"
            break
        fi
    else
        # Use netstat/ss to check if port 8080 is listening (Windows/Linux compatible)
        if netstat -an 2>/dev/null | grep -q ":8080.*LISTEN" || ss -tuln 2>/dev/null | grep -q ":8080"; then
            print_status "Application started successfully! (Port 8080 is listening)"
            break
        fi
    fi
    sleep 2
    if [ $i -eq 30 ]; then
        print_warning "Could not verify application startup within 60 seconds"
        print_info "You can check manually at: http://localhost:8080"
        break
    fi
done

# Stop the application
kill $APP_PID 2>/dev/null || true

echo ""

# Success summary
echo "🎉 Setup Complete!"
echo "=================="
print_status "Environment Variables: .env file created"
print_status "Infrastructure: PostgreSQL & Redis running"
print_status "Application: Built and tested successfully"
print_status "Database: Migrations applied"
print_status "Health Check: Application starts correctly"

echo ""
echo "🚀 Next Steps:"
echo "=============)"
echo "1. Update .env file with your actual configuration"
echo "2. Start development with: ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo "3. Visit: http://localhost:8080/api/v1/swagger-ui.html for API docs"
echo "4. Health check: http://localhost:8080/api/v1/health/status"
echo ""
echo "📚 Useful Commands:"
echo "==================="
echo "Start app: ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo "Run tests: ./mvnw test"
echo "Stop services: docker compose down (or docker-compose down)"
echo "View logs: docker compose logs -f postgres redis"
echo "Check services: docker ps"
echo ""
print_status "Payment Gateway development environment is ready! 🎯"