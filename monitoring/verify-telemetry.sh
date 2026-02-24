#!/bin/bash

# Script to verify backend telemetry is working

set -e

echo "========================================="
echo "Verifying Backend Telemetry"
echo "========================================="
echo ""

# Check backend is running
echo "1. Checking backend health..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "   ✓ Backend is running"
    curl -s http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null || echo "   Status: UP"
else
    echo "   ✗ Backend is not running on localhost:8080"
    exit 1
fi

echo ""
echo "2. Checking Prometheus metrics endpoint..."
METRICS_COUNT=$(curl -s http://localhost:8080/actuator/prometheus | grep -c "^# HELP" || echo "0")
if [ "$METRICS_COUNT" -gt 0 ]; then
    echo "   ✓ Metrics endpoint is accessible ($METRICS_COUNT metric types found)"
else
    echo "   ✗ No metrics found"
    exit 1
fi

echo ""
echo "3. Generating test traffic to create metrics..."
for i in {1..3}; do
    curl -s http://localhost:8080/api/products > /dev/null
    curl -s http://localhost:8080/api/categories > /dev/null
    echo "   Request $i completed"
done

echo ""
echo "4. Checking Prometheus connectivity..."
if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo "   ✓ Prometheus is running"
    
    # Check if backend target is up
    TARGET_STATUS=$(curl -s http://localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] | select(.job=="backend") | .health' 2>/dev/null || echo "unknown")
    if [ "$TARGET_STATUS" = "up" ]; then
        echo "   ✓ Backend target is UP in Prometheus"
    elif [ "$TARGET_STATUS" = "down" ]; then
        echo "   ✗ Backend target is DOWN in Prometheus"
        echo "   Check: http://localhost:9090/targets"
    else
        echo "   ⚠ Could not determine target status"
    fi
else
    echo "   ✗ Prometheus is not running on localhost:9090"
fi

echo ""
echo "5. Sample metrics available:"
echo "   JVM Metrics:"
curl -s http://localhost:8080/actuator/prometheus | grep "^jvm_memory_used_bytes" | head -2 || echo "   (none yet)"
echo ""
echo "   HTTP Metrics:"
curl -s http://localhost:8080/actuator/prometheus | grep "^http_server_requests_seconds_count" | head -2 || echo "   (make more API calls)"
echo ""
echo "   Custom Metrics:"
curl -s http://localhost:8080/actuator/prometheus | grep "^products_\|^categories_" | head -3 || echo "   (will appear after API operations)"

echo ""
echo "========================================="
echo "Verification Complete!"
echo "========================================="
echo ""
echo "Next Steps:"
echo "1. Open Prometheus: http://localhost:9090"
echo "2. Go to Status → Targets to verify backend is UP"
echo "3. Go to Graph and try query: up{job=\"backend\"}"
echo "4. Open Grafana: http://localhost:3001"
echo "5. Check dashboards for data (may take 10-15 seconds)"
echo ""
