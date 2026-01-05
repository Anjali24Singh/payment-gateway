#!/bin/bash

# Payment Gateway - Local Development Startup Script
# This script helps you start the application in development mode

set -e

echo "🚀 Payment Gateway - Development Mode Startup"
echo "=============================================="

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if a port is in use
port_in_use() {
    lsof -i :$1 >/dev/null 2>&1
}

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command_exists docker; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command_exists docker-compose; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

if ! command_exists java; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Prerequisites check passed"

# Check for port conflicts
echo "🔍 Checking for port conflicts..."
PORTS_TO_CHECK=(5433 6380 8080 9090 3000 9411)
for port in "${PORTS_TO_CHECK[@]}"; do
    if port_in_use $port; then
        echo "⚠️  Port $port is already in use. Please stop the service using this port."
        echo "   You can find what's using the port with: lsof -i :$port"
        read -p "Do you want to continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
done

echo "✅ Port check completed"

# Option selection
echo ""
echo "🎯 Choose how to run the application:"
echo "1) Full Stack (with Docker - PostgreSQL, Redis, Monitoring)"
echo "2) Application Only (requires external DB and Redis)"
echo "3) Infrastructure Only (just start DB and Redis)"
echo "4) Clean Start (remove existing containers and volumes)"

read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        echo "🐳 Starting full stack with Docker..."
        docker-compose up --build -d
        echo "✅ Services started successfully!"
        echo ""
        echo "📊 Access URLs:"
        echo "   Application:     http://localhost:8080/api/v1"
        echo "   Swagger UI:      http://localhost:8080/api/v1/swagger-ui.html"
        echo "   Health Check:    http://localhost:8080/api/v1/actuator/health"
        echo "   Prometheus:      http://localhost:9090"
        echo "   Grafana:         http://localhost:3000 (admin/admin)"
        echo "   Zipkin:          http://localhost:9411"
        echo ""
        echo "📋 To view logs: docker-compose logs -f payment-gateway"
        echo "📋 To stop: docker-compose down"
        ;;
    2)
        echo "☕ Starting application only..."
        echo "⚠️  Make sure PostgreSQL is running on localhost:5433"
        echo "⚠️  Make sure Redis is running on localhost:6380"
        echo ""
        export SPRING_PROFILES_ACTIVE=dev
        if command_exists ./mvnw; then
            ./mvnw spring-boot:run
        else
            mvn spring-boot:run
        fi
        ;;
    3)
        echo "🛠️  Starting infrastructure only..."
        docker-compose up -d postgres redis
        echo "✅ Infrastructure started!"
        echo "   PostgreSQL: localhost:5433"
        echo "   Redis:      localhost:6380"
        echo ""
        echo "Now you can run the application with:"
        echo "   ./run-dev.sh and choose option 2"
        ;;
    4)
        echo "🧹 Cleaning up existing containers and volumes..."
        docker-compose down -v
        docker system prune -f
        echo "✅ Cleanup completed!"
        echo "Now you can run with a fresh start using option 1"
        ;;
    *)
        echo "❌ Invalid choice. Please run the script again."
        exit 1
        ;;
esac

echo ""
echo "🎉 Done! Happy coding!"
