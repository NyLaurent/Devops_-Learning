#!/bin/bash

# Start Monitoring Stack
# This script starts Prometheus and Grafana for monitoring the E-Commerce application

set -e

echo "========================================="
echo "Starting Monitoring Stack"
echo "========================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if backend is running (optional check)
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Warning: Backend is not running on localhost:8080"
    echo "Make sure to start the backend before viewing metrics in Grafana"
    echo ""
fi

# Check/create networks
echo "Checking networks..."
if ! docker network ls | grep -q "ecommerce-network"; then
    echo "Creating ecommerce-network..."
    docker network create ecommerce-network 2>/dev/null || true
fi

if ! docker network ls | grep -q "^.*monitoring"; then
    echo "Creating monitoring network..."
    docker network create monitoring 2>/dev/null || true
fi

# Start monitoring stack
echo "Starting Prometheus and Grafana..."
docker-compose up -d

echo ""
echo "Waiting for services to be ready..."
sleep 5

# Check Prometheus
if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo "✓ Prometheus is running at http://localhost:9090"
else
    echo "✗ Prometheus failed to start"
fi

# Check Grafana
if curl -s http://localhost:3001/api/health > /dev/null 2>&1; then
    echo "✓ Grafana is running at http://localhost:3001"
else
    echo "✗ Grafana failed to start"
fi

echo ""
echo "========================================="
echo "Monitoring Stack Started!"
echo "========================================="
echo ""
echo "Access Points:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana:    http://localhost:3001"
echo "  - Backend Metrics: http://localhost:8080/actuator/prometheus"
echo ""
echo "Grafana Credentials:"
echo "  Username: admin"
echo "  Password: admin"
echo ""
echo "Dashboards:"
echo "  - Backend Overview"
echo "  - Application Metrics"
echo ""
echo "To stop the monitoring stack:"
echo "  docker-compose down"
echo ""
