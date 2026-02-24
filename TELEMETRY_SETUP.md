# Backend Telemetry Setup Verification

## ✅ Telemetry Dependencies Confirmed

The backend **DOES have telemetry** configured:

### pom.xml (Lines 47-51)
```xml
<!-- Micrometer Prometheus for metrics export -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### application.yml
- Prometheus endpoint enabled: `/actuator/prometheus`
- Metrics export configured
- Percentiles and histograms enabled

### Custom Metrics Code
- `MetricsConfig.java` - Registers custom metrics
- `ProductService.java` - Tracks product operations
- `CategoryService.java` - Tracks category operations

## Current Issues

### 1. Port 5432 Conflict
**Problem**: PostgreSQL port 5432 is already in use

**Solution**: Updated `docker-compose.yml` to use port **5433** on host:
```yaml
ports:
  - "5433:5432"  # Host:Container
```

**To use existing PostgreSQL** (if you prefer):
1. Stop the existing PostgreSQL service
2. Or update `docker-compose.yml` to not expose the port:
   ```yaml
   # Remove ports section or comment it out
   # ports:
   #   - "5433:5432"
   ```
3. Update backend environment to use your existing PostgreSQL

### 2. Backend Not Running
**Problem**: `curl http://localhost:8080/actuator/prometheus` fails

**Solutions**:

**Option A: Start with Docker Compose**
```bash
# Fix port conflict first, then:
docker-compose up -d backend
```

**Option B: Run Backend Locally (for development)**
```bash
cd backend
mvn spring-boot:run
```

**Option C: Use Existing PostgreSQL**
If you have PostgreSQL running locally on 5432:
1. Update `application.yml` or set environment variables:
   ```bash
   export POSTGRES_HOST=localhost
   export POSTGRES_PORT=5432
   export POSTGRES_DB=ecommerce
   export POSTGRES_USER=your_user
   export POSTGRES_PASSWORD=your_password
   ```
2. Run backend: `mvn spring-boot:run`

## Verification Steps

### 1. Check Backend is Running
```bash
curl http://localhost:8080/actuator/health
```

### 2. Check Metrics Endpoint
```bash
curl http://localhost:8080/actuator/prometheus | head -20
```

**Expected Output**: Prometheus metrics format:
```
# HELP jvm_memory_used_bytes...
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="product-service",...} ...
```

### 3. Check Prometheus Can Scrape
```bash
# From Prometheus container
docker exec prometheus wget -O- http://host.docker.internal:8080/actuator/prometheus | head -10
```

### 4. Verify in Prometheus UI
1. Open: http://localhost:9090
2. Go to: **Status** → **Targets**
3. Check: `backend` target should be **UP** (green)

### 5. Check Custom Metrics
After making API calls:
```bash
curl http://localhost:8080/actuator/prometheus | grep "products_\|categories_"
```

**Expected Metrics**:
- `products_created_total`
- `products_updated_total`
- `products_viewed_total`
- `categories_created_total`
- `categories_updated_total`
- `categories_viewed_total`

## Quick Fix Commands

### Fix Port Conflict and Start Services

```bash
# Stop any existing containers
docker-compose down

# Start services (will use port 5433 for PostgreSQL)
docker-compose up -d

# Check backend logs
docker logs ecommerce-backend

# Verify backend is running
curl http://localhost:8080/actuator/health
```

### If Backend Fails to Start

**Check logs:**
```bash
docker logs ecommerce-backend
```

**Common issues:**
1. **Database connection failed**: Check PostgreSQL is running and accessible
2. **Port already in use**: Check what's using port 8080: `lsof -i :8080`
3. **Build failed**: Rebuild: `docker-compose build backend`

### Rebuild Backend (if needed)

```bash
cd backend
mvn clean package
cd ..
docker-compose build backend
docker-compose up -d backend
```

## Telemetry Metrics Available

Once backend is running, these metrics will be available:

### Automatic Metrics (Spring Boot Actuator)
- **HTTP**: `http_server_requests_seconds_*`
- **JVM**: `jvm_memory_*`, `jvm_threads_*`, `jvm_gc_*`
- **System**: `process_cpu_usage`, `system_cpu_count`
- **Database**: `hikari_connections_*`

### Custom Business Metrics
- `products_created_total`
- `products_updated_total`
- `products_viewed_total`
- `products_query_duration_seconds`
- `categories_created_total`
- `categories_updated_total`
- `categories_viewed_total`

## Next Steps

1. **Fix port conflict**: Use port 5433 or stop existing PostgreSQL
2. **Start backend**: `docker-compose up -d backend`
3. **Verify metrics**: `curl http://localhost:8080/actuator/prometheus`
4. **Check Prometheus**: http://localhost:9090/targets
5. **View in Grafana**: http://localhost:3001 (wait 10-15 seconds for data)

## Troubleshooting

See `monitoring/TROUBLESHOOTING.md` for detailed troubleshooting guide.
