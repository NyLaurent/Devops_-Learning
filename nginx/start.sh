#!/bin/bash

# Start Nginx Reverse Proxy
# This script starts Nginx with reverse proxy configuration

set -e

echo "========================================="
echo "Starting Nginx Reverse Proxy"
echo "========================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if hosts file is configured
if ! grep -q "api.microservices" /etc/hosts 2>/dev/null; then
    echo "Warning: Hosts file not configured!"
    echo "Run: sudo ./setup-hosts.sh"
    echo "Or manually add to /etc/hosts:"
    echo "  127.0.0.1 api.microservices"
    echo "  127.0.0.1 app.microservices"
    echo "  127.0.0.1 monitoring.microservices"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check/create networks
echo "Checking networks..."
if ! docker network ls | grep -qE "(^| )ecommerce-network( |$)"; then
    echo "Creating ecommerce-network..."
    docker network create ecommerce-network 2>/dev/null || true
fi

if ! docker network ls | grep -qE "(^| )monitoring( |$)"; then
    echo "Creating monitoring network..."
    docker network create monitoring 2>/dev/null || true
fi

# Test Nginx configuration
echo "Testing Nginx configuration..."
docker run --rm \
    -v "$(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro" \
    -v "$(pwd)/conf.d:/etc/nginx/conf.d:ro" \
    nginx:alpine nginx -t

if [ $? -ne 0 ]; then
    echo "Error: Nginx configuration test failed!"
    exit 1
fi

# Start Nginx
echo "Starting Nginx..."
docker-compose up -d

echo ""
echo "Waiting for Nginx to be ready..."
sleep 3

# Check if Nginx is running
if curl -s http://localhost/health > /dev/null 2>&1; then
    echo "✓ Nginx is running"
else
    echo "⚠ Nginx started but health check failed"
fi

echo ""
echo "========================================="
echo "Nginx Reverse Proxy Started!"
echo "========================================="
echo ""
echo "Access Points:"
echo "  - Frontend:    http://app.microservices"
echo "  - Backend API: http://api.microservices/api"
echo "  - Swagger:     http://api.microservices/swagger-ui.html"
echo "  - Prometheus:  http://monitoring.microservices/prometheus"
echo "  - Grafana:     http://monitoring.microservices/grafana"
echo ""
echo "To stop Nginx:"
echo "  docker-compose down"
echo ""
echo "To view logs:"
echo "  docker logs ecommerce-nginx"
echo "  docker logs -f ecommerce-nginx  # Follow logs"
echo ""
