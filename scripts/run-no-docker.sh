#!/bin/bash

# Payment Gateway - No Docker Setup Script
# Runs the application using H2 in-memory database

echo "🚀 Payment Gateway - No Docker Setup"
echo "===================================="
echo ""

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✅ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️ $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️ $1${NC}"; }

# Check prerequisites (no Docker needed)
echo "🛠️ Checking Prerequisites..."
echo "----------------------------"

# Check Java
if command -v java >/dev/null 2>&1; then
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print_status "Java $java_version found"
else
    print_error "Java not found. Please install Java 17+"
    exit 1
fi

# Check Maven
if command -v mvn >/dev/null 2>&1; then
    maven_version=$(mvn -version | head -n 1 | cut -d ' ' -f 3)
    print_status "Maven $maven_version found"
else
    print_info "Using Maven wrapper (./mvnw)"
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

echo ""

# Run tests
echo "🧪 Running Unit Tests..."
echo "------------------------"

print_info "Running unit tests..."
if ./mvnw test -Dtest="*UnitTest" -Dspring.profiles.active=no-docker; then
    print_status "Unit tests passed"
else
    print_warning "Some unit tests may have failed. This is expected without full infrastructure."
fi

echo ""

# Start application
echo "🚀 Starting Application..."
echo "-------------------------"

print_status "Starting Payment Gateway with H2 database..."
print_info "Application will be available at: http://localhost:8080"
print_info "H2 Console available at: http://localhost:8080/h2-console"
print_info "Health Check: http://localhost:8080/actuator/health"
print_info ""
print_warning "Press Ctrl+C to stop the application"
print_info ""

# Start with no-docker profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=no-docker