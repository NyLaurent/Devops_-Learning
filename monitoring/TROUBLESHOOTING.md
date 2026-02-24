# Monitoring Troubleshooting Guide

## Issue: "No Data" in Grafana

If you see "No data" in Grafana dashboards, follow these steps:

### Step 1: Verify Backend is Running

```bash
# Check if backend is running
curl http://localhost:8080/actuator/health

# Check if metrics endpoint is accessible
curl http://localhost:8080/actuator/prometheus | head -20
```

**Expected**: Should return Prometheus metrics format

### Step 2: Check Prometheus Targets

1. Open Prometheus: http://localhost:9090
2. Go to **Status** → **Targets**
3. Check if `backend` target is **UP** (green) or **DOWN** (red)

**If DOWN:**
- Check the error message
- Verify the target URL is correct
- Ensure backend is accessible from Prometheus container

### Step 3: Verify Network Connectivity

**From Prometheus container:**
```bash
docker exec prometheus wget -O- http://host.docker.internal:8080/actuator/prometheus
```

**If this fails**, try:
```bash
# If backend is in same Docker network
docker exec prometheus wget -O- http://backend:8080/actuator/prometheus
```

### Step 4: Check Prometheus Configuration

Verify `prometheus/prometheus.yml` has correct target:

```yaml
- job_name: 'backend'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['host.docker.internal:8080']  # Docker Desktop
      # OR
    - targets: ['backend:8080']  # Same network
```

### Step 5: Generate Some Traffic

Metrics only appear after requests are made. Generate some API calls:

```bash
# Make some API calls to generate metrics
curl http://localhost:8080/api/products
curl http://localhost:8080/api/categories
curl http://localhost:8080/api/products/1
```

Wait 10-15 seconds, then check Prometheus again.

### Step 6: Check Prometheus Logs

```bash
docker logs prometheus
```

Look for scrape errors or connection issues.

### Step 7: Verify Metrics in Prometheus

1. Open Prometheus: http://localhost:9090
2. Go to **Graph** tab
3. Try these queries:
   - `up{job="backend"}` - Should return 1 if backend is up
   - `jvm_memory_used_bytes` - JVM memory metrics
   - `http_server_requests_seconds_count` - HTTP request count
   - `products_created_total` - Custom product metrics

### Step 8: Reload Prometheus Configuration

```bash
# Reload Prometheus config
curl -X POST http://localhost:9090/-/reload

# Or restart Prometheus
docker restart prometheus
```

## Common Issues and Solutions

### Issue: "connection refused" in Prometheus

**Cause**: Backend not accessible from Prometheus container

**Solutions**:
1. **Docker Desktop**: Use `host.docker.internal:8080`
2. **Same Network**: Use `backend:8080` and ensure both are in `ecommerce-network`
3. **Linux Docker**: May need to use host IP or add `--network host`

**Fix**:
```bash
# Ensure backend is in the network
docker network connect ecommerce-network ecommerce-backend

# Or update prometheus.yml to use correct target
```

### Issue: "404 Not Found" when scraping

**Cause**: Wrong metrics path

**Solution**: Verify path is `/actuator/prometheus` in `prometheus.yml`

### Issue: Metrics exist but Grafana shows "No data"

**Cause**: Time range or query issue

**Solutions**:
1. Check time range in Grafana (top right)
2. Verify query syntax in dashboard panels
3. Check if metrics have the expected labels

### Issue: Only JVM metrics, no HTTP metrics

**Cause**: No HTTP requests made yet

**Solution**: Make some API calls:
```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/categories
```

### Issue: Custom metrics not appearing

**Cause**: Metrics not registered or service not called

**Solution**:
1. Verify `MetricsConfig` is loaded
2. Make API calls that trigger the metrics
3. Check Prometheus for metric names:
   - `products_created_total`
   - `categories_created_total`

## Verification Checklist

- [ ] Backend is running and accessible
- [ ] `/actuator/prometheus` endpoint returns data
- [ ] Prometheus target shows as UP
- [ ] Some API requests have been made
- [ ] Time range in Grafana includes current time
- [ ] Prometheus queries return data
- [ ] Networks are properly connected

## Quick Test Script

```bash
#!/bin/bash
echo "Testing Monitoring Setup..."

echo "1. Backend Health:"
curl -s http://localhost:8080/actuator/health | jq

echo "2. Prometheus Metrics:"
curl -s http://localhost:8080/actuator/prometheus | grep -c "# HELP"

echo "3. Prometheus Target Status:"
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.job=="backend")'

echo "4. Generate Test Traffic:"
for i in {1..5}; do
    curl -s http://localhost:8080/api/products > /dev/null
    curl -s http://localhost:8080/api/categories > /dev/null
done

echo "5. Check Metrics in Prometheus:"
echo "   Open: http://localhost:9090/graph"
echo "   Query: up{job=\"backend\"}"
```

## Still Having Issues?

1. Check all service logs:
   ```bash
   docker logs ecommerce-backend
   docker logs prometheus
   docker logs grafana
   ```

2. Verify network connectivity:
   ```bash
   docker network inspect ecommerce-network
   docker network inspect monitoring
   ```

3. Restart services:
   ```bash
   docker-compose restart
   ```

4. Check firewall/security settings

5. Review Prometheus configuration syntax:
   ```bash
   docker exec prometheus promtool check config /etc/prometheus/prometheus.yml
   ```
