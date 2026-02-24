# Monitoring Stack: Prometheus & Grafana

This directory contains the monitoring infrastructure for the E-Commerce application using Prometheus for metrics collection and Grafana for visualization.

## Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Backend   │─────▶│  Prometheus  │─────▶│   Grafana   │
│  (Spring    │      │  (Metrics    │      │(Dashboards) │
│   Boot)     │      │  Collector)  │      │             │
└─────────────┘      └──────────────┘      └─────────────┘
     │                       │
     │                       │
     └───────────────────────┘
      /actuator/prometheus
```

## Components

### 1. Prometheus
- **Purpose**: Metrics collection and storage
- **Port**: 9090
- **URL**: http://localhost:9090
- **Metrics Endpoint**: Scrapes from backend at `/actuator/prometheus`

### 2. Grafana
- **Purpose**: Metrics visualization and dashboards
- **Port**: 3001 (to avoid conflict with frontend on 3000)
- **URL**: http://localhost:3001
- **Default Credentials**:
  - Username: `admin`
  - Password: `admin`

### 3. Node Exporter (Optional)
- **Purpose**: Host system metrics
- **Port**: 9100

## Quick Start

### 1. Start Monitoring Stack

```bash
cd monitoring
docker-compose up -d
```

### 2. Verify Services

**Prometheus:**
```bash
curl http://localhost:9090/-/healthy
```

**Grafana:**
```bash
curl http://localhost:3001/api/health
```

**Backend Metrics:**
```bash
curl http://localhost:8080/actuator/prometheus
```

### 3. Access Dashboards

1. Open Grafana: http://localhost:3001
2. Login with `admin`/`admin`
3. Navigate to **Dashboards** → **Backend Overview** or **Application Metrics**

## Configuration

### Prometheus Configuration

**File**: `prometheus/prometheus.yml`

Key settings:
- `scrape_interval`: How often to scrape metrics (15s)
- `evaluation_interval`: How often to evaluate alert rules (15s)
- `scrape_configs`: List of targets to scrape

**Backend Scraping:**
- Job name: `backend`
- Target: `host.docker.internal:8080` (for Docker Desktop)
- Metrics path: `/actuator/prometheus`
- Scrape interval: 10s

**For Linux or different Docker setup**, update the target:
```yaml
static_configs:
  - targets: ['backend:8080']  # If backend is in same Docker network
  # OR
  - targets: ['host.docker.internal:8080']  # Docker Desktop
  # OR
  - targets: ['localhost:8080']  # Local development
```

### Grafana Configuration

**Data Source**: Auto-provisioned from `grafana/provisioning/datasources/prometheus.yml`

**Dashboards**: Auto-provisioned from `grafana/dashboards/`

### Backend Configuration

The backend is configured to expose Prometheus metrics via Spring Boot Actuator.

**Dependencies** (already added):
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

**Configuration** (`application.yml`):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## Available Metrics

### HTTP Metrics (Auto-generated)
- `http_server_requests_seconds`: Request duration histogram
- `http_server_requests_seconds_count`: Total request count
- `http_server_requests_seconds_sum`: Total request duration
- **Note**: These metrics only appear after HTTP requests are made

### JVM Metrics (Always Available)
- `jvm_memory_used_bytes`: Memory usage by area
- `jvm_memory_max_bytes`: Maximum memory
- `jvm_threads_live_threads`: Active threads
- `jvm_gc_pause_seconds`: GC pause time
- `process_cpu_usage`: CPU usage percentage
- `jvm_buffer_count_buffers`: Buffer counts
- `system_cpu_count`: Number of CPU cores

### Application Metrics
- `hikari_connections_active`: Active database connections
- `hikari_connections_idle`: Idle database connections
- `hikari_connections_pending`: Pending connections

### Custom Business Metrics (Now Available)
The backend now includes custom metrics for business operations:

- `products_created_total`: Total products created
- `products_updated_total`: Total products updated
- `products_viewed_total`: Total product views
- `categories_created_total`: Total categories created
- `categories_updated_total`: Total categories updated
- `categories_viewed_total`: Total category views
- `products_query_duration_seconds`: Product query execution time

These metrics are automatically registered and will appear in Prometheus after the services are used.

### Custom Metrics Implementation
Custom metrics are implemented in:
- `MetricsConfig.java`: Registers custom counters and timers
- `ProductService.java`: Tracks product operations
- `CategoryService.java`: Tracks category operations

### Adding More Custom Metrics
You can add custom metrics in your Spring Boot application:

```java
@Service
public class ProductService {
    private final Counter productCreatedCounter;
    private final Timer productQueryTimer;
    
    public ProductService(MeterRegistry meterRegistry) {
        this.productCreatedCounter = Counter.builder("products.created")
            .description("Number of products created")
            .register(meterRegistry);
        
        this.productQueryTimer = Timer.builder("products.query.time")
            .description("Product query time")
            .register(meterRegistry);
    }
    
    public ProductDto createProduct(ProductDto dto) {
        // ... create logic
        productCreatedCounter.increment();
        return createdProduct;
    }
    
    public List<ProductDto> getAllProducts() {
        return productQueryTimer.recordCallable(() -> {
            // ... query logic
            return products;
        });
    }
}
```

## Dashboards

### 1. Backend Overview
**File**: `grafana/dashboards/backend-overview.json`

**Panels:**
- Request Rate (requests/second)
- Response Time (95th percentile)
- Error Rate
- JVM Memory Usage
- Active Threads
- CPU Usage
- HTTP Status Codes (pie chart)
- Database Connections

### 2. Application Metrics
**File**: `grafana/dashboards/application-metrics.json`

**Panels:**
- Response Time Distribution (heatmap)
- Request Duration by Endpoint (p50, p95, p99)
- Request Count by Method
- JVM GC Pause Time
- JVM Memory Pools
- System Load

## Alerting

Prometheus alert rules are defined in `prometheus/rules/backend-alerts.yml`.

**Current Alerts:**
1. **HighErrorRate**: Error rate > 5% for 5 minutes
2. **HighResponseTime**: 95th percentile > 2s for 5 minutes
3. **ApplicationDown**: Application unavailable for 1 minute
4. **HighMemoryUsage**: Memory usage > 90% for 5 minutes
5. **HighCPUUsage**: CPU usage > 80% for 5 minutes

**To enable alerting**, uncomment the alertmanager configuration in `prometheus.yml` and set up Alertmanager.

## Integration with Docker Compose

To integrate with the main application:

**Option 1: Add to main docker-compose.yml**
```yaml
services:
  # ... existing services ...
  
  prometheus:
    # ... (copy from monitoring/docker-compose.yml)
  
  grafana:
    # ... (copy from monitoring/docker-compose.yml)
```

**Option 2: Use external network**
```bash
# Create network
docker network create ecommerce-network

# Start main app
docker-compose up -d

# Start monitoring (will join network)
cd monitoring
docker-compose up -d
```

## Troubleshooting

### Prometheus can't scrape backend

**Issue**: Prometheus shows backend as down

**Solutions**:
1. Check backend is running: `curl http://localhost:8080/actuator/health`
2. Verify metrics endpoint: `curl http://localhost:8080/actuator/prometheus`
3. Update Prometheus target in `prometheus.yml`:
   - Docker Desktop: `host.docker.internal:8080`
   - Linux Docker: `backend:8080` (if in same network)
   - Local: `localhost:8080`

### Grafana can't connect to Prometheus

**Issue**: "Data source is not working"

**Solutions**:
1. Check Prometheus is running: `curl http://localhost:9090/-/healthy`
2. Verify network connectivity from Grafana container
3. Check datasource URL in `grafana/provisioning/datasources/prometheus.yml`

### No metrics appearing

**Issue**: Dashboards show "No data"

**Solutions**:
1. Verify backend is exposing metrics: `curl http://localhost:8080/actuator/prometheus`
2. Check Prometheus targets: http://localhost:9090/targets
3. Verify scrape configuration in Prometheus
4. Check time range in Grafana (should include current time)

### High resource usage

**Issue**: Monitoring stack using too much resources

**Solutions**:
1. Increase scrape interval in `prometheus.yml`
2. Reduce retention period: `--storage.tsdb.retention.time=7d`
3. Disable unnecessary dashboards
4. Use sampling for high-volume metrics

## Production Considerations

### Security

1. **Change default passwords**:
   ```yaml
   environment:
     - GF_SECURITY_ADMIN_PASSWORD=your-secure-password
   ```

2. **Enable authentication** in Prometheus (requires reverse proxy)

3. **Use HTTPS** for production

4. **Restrict network access**:
   - Only expose necessary ports
   - Use firewall rules
   - Use VPN for Grafana access

### Performance

1. **Increase retention** based on needs:
   ```yaml
   command:
     - '--storage.tsdb.retention.time=90d'
   ```

2. **Configure resource limits**:
   ```yaml
   deploy:
     resources:
       limits:
         memory: 2G
         cpus: '1'
   ```

3. **Use remote storage** for long-term retention:
   - Prometheus → Thanos
   - Prometheus → Cortex
   - Prometheus → VictoriaMetrics

### High Availability

1. **Run multiple Prometheus instances** (federation)

2. **Use Grafana HA** setup

3. **Backup Grafana dashboards**:
   ```bash
   docker exec grafana grafana-cli admin export-dashboard
   ```

## Useful Queries

### Request Rate
```promql
rate(http_server_requests_seconds_count[5m])
```

### Error Rate
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### 95th Percentile Response Time
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

### Memory Usage Percentage
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

### Top 5 Slowest Endpoints
```promql
topk(5, histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])))
```

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Gatling Documentation](https://grafana.com/docs/grafana/latest/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer](https://micrometer.io/docs)

## Support

For issues or questions:
1. Check logs: `docker-compose logs prometheus grafana`
2. Verify configuration files
3. Test endpoints manually
4. Check network connectivity
