#!/bin/bash

# Payment Gateway - Monitoring Stack Management Script
# This script manages the complete monitoring infrastructure

set -e

echo "📊 Payment Gateway - Monitoring Stack Manager"
echo "=============================================="
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

# Function to show usage
show_usage() {
    echo "Usage: $0 [start|stop|restart|status|logs|urls]"
    echo ""
    echo "Commands:"
    echo "  start    - Start monitoring stack (Prometheus, Grafana, Zipkin)"
    echo "  stop     - Stop monitoring stack"
    echo "  restart  - Restart monitoring stack"
    echo "  status   - Show status of monitoring services"
    echo "  logs     - Show logs for monitoring services"
    echo "  urls     - Show URLs for accessing monitoring services"
    echo ""
}

# Function to start monitoring stack
start_monitoring() {
    print_info "Starting monitoring stack..."
    
    # Start Prometheus, Grafana, and Zipkin
    if docker compose up -d prometheus grafana zipkin 2>/dev/null || docker-compose up -d prometheus grafana zipkin; then
        :  # Success
    else
        print_error "Failed to start monitoring services"
        return 1
    fi
    
    if [ $? -eq 0 ]; then
        print_status "Monitoring stack started successfully!"
        print_info "Waiting for services to be ready..."
        sleep 15
        
        # Check service health
        check_service_health
        show_urls
    else
        print_error "Failed to start monitoring stack"
        exit 1
    fi
}

# Function to stop monitoring stack
stop_monitoring() {
    print_info "Stopping monitoring stack..."
    
    if docker compose stop prometheus grafana zipkin 2>/dev/null || docker-compose stop prometheus grafana zipkin; then
        print_status "Monitoring stack stopped successfully!"
    else
        print_error "Failed to stop monitoring stack"
        exit 1
    fi
}

# Function to restart monitoring stack
restart_monitoring() {
    print_info "Restarting monitoring stack..."
    stop_monitoring
    sleep 5
    start_monitoring
}

# Function to check service status
check_status() {
    echo "🔍 Monitoring Services Status:"
    echo "=============================="
    
    services=("payment-gateway-prometheus" "payment-gateway-grafana" "payment-gateway-zipkin")
    
    for service in "${services[@]}"; do
        if docker ps --filter "name=$service" --filter "status=running" | grep -q "$service"; then
            print_status "$service is running"
        else
            print_error "$service is not running"
        fi
    done
    
    echo ""
    check_service_health
}

# Function to check service health
check_service_health() {
    print_info "Checking service health..."
    
    # Check Prometheus
    if curl -s http://localhost:9090/-/healthy >/dev/null 2>&1; then
        print_status "Prometheus is healthy"
    else
        print_warning "Prometheus health check failed"
    fi
    
    # Check Grafana
    if curl -s http://localhost:3000/api/health >/dev/null 2>&1; then
        print_status "Grafana is healthy"
    else
        print_warning "Grafana health check failed"
    fi
    
    # Check Zipkin
    if curl -s http://localhost:9411/health >/dev/null 2>&1; then
        print_status "Zipkin is healthy"
    else
        print_warning "Zipkin health check failed"
    fi
}

# Function to show logs
show_logs() {
    echo "📋 Recent logs for monitoring services:"
    echo "======================================"
    
    print_info "Prometheus logs:"
    docker compose logs --tail=20 prometheus 2>/dev/null || docker-compose logs --tail=20 prometheus
    
    echo ""
    print_info "Grafana logs:"
    docker compose logs --tail=20 grafana 2>/dev/null || docker-compose logs --tail=20 grafana
    
    echo ""
    print_info "Zipkin logs:"
    docker compose logs --tail=20 zipkin 2>/dev/null || docker-compose logs --tail=20 zipkin
}

# Function to show service URLs
show_urls() {
    echo ""
    echo "🌐 Monitoring Service URLs:"
    echo "=========================="
    echo "📊 Prometheus:    http://localhost:9090"
    echo "📈 Grafana:       http://localhost:3000 (admin/admin)"
    echo "🔍 Zipkin:        http://localhost:9411"
    echo "📊 App Metrics:   http://localhost:8080/api/v1/actuator/prometheus"
    echo "❤️ App Health:    http://localhost:8080/api/v1/health/status"
    echo ""
    echo "📚 Grafana Default Dashboards:"
    echo "  • JVM Metrics Dashboard"
    echo "  • Spring Boot Dashboard" 
    echo "  • Payment Gateway Business Metrics"
    echo ""
}

# Main script logic
case "$1" in
    start)
        start_monitoring
        ;;
    stop)
        stop_monitoring
        ;;
    restart)
        restart_monitoring
        ;;
    status)
        check_status
        ;;
    logs)
        show_logs
        ;;
    urls)
        show_urls
        ;;
    "")
        show_usage
        check_status
        ;;
    *)
        echo "Unknown command: $1"
        show_usage
        exit 1
        ;;
esac