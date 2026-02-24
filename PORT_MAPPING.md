# Port Mapping Reference

This document lists all ports used across the E-Commerce application stack to ensure no conflicts.

## Port Allocation Summary

| Port | Service | Component | Status | Notes |
|------|---------|-----------|--------|-------|
| 80 | Nginx | Load Balancer | Optional | Profile: `production` |
| 3000 | Frontend | React App | Active | Main frontend |
| 3001 | Grafana | Monitoring | Active | **Intentionally 3001 to avoid conflict with frontend** |
| 5432 | PostgreSQL | Database | Active | Standard PostgreSQL port |
| 6379 | Redis | Cache | Optional | Profile: `cache` |
| 8080 | Backend | Spring Boot API | Active | Main API server |
| 9090 | Prometheus | Metrics | Active | Monitoring |
| 9100 | Node Exporter | Host Metrics | Active | Monitoring |

## Detailed Port Breakdown

### Application Services

#### Backend (Spring Boot)
- **Port**: `8080`
- **File**: `backend/src/main/resources/application.yml`
- **Endpoints**:
  - API: `http://localhost:8080/api/*`
  - Actuator: `http://localhost:8080/actuator/*`
  - Prometheus Metrics: `http://localhost:8080/actuator/prometheus`
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
- **Docker**: `docker-compose.yml` → `backend` service
- **Status**: ✅ No conflicts

#### Frontend (React)
- **Port**: `3000`
- **File**: `docker-compose.yml` → `frontend` service
- **URL**: `http://localhost:3000`
- **Status**: ✅ No conflicts

#### PostgreSQL Database
- **Port**: `5432`
- **File**: `docker-compose.yml` → `postgres` service
- **Connection**: `localhost:5432`
- **Status**: ✅ No conflicts (standard PostgreSQL port)

#### Redis (Optional)
- **Port**: `6379`
- **File**: `docker-compose.yml` → `redis` service
- **Profile**: `cache` (only starts with `--profile cache`)
- **Status**: ✅ No conflicts (standard Redis port)

#### Nginx (Optional)
- **Port**: `80`
- **File**: `docker-compose.yml` → `nginx` service
- **Profile**: `production` (only starts with `--profile production`)
- **Status**: ✅ No conflicts (standard HTTP port)

### Monitoring Services

#### Prometheus
- **Port**: `9090`
- **File**: `monitoring/docker-compose.yml` → `prometheus` service
- **URL**: `http://localhost:9090`
- **Status**: ✅ No conflicts

#### Grafana
- **Port**: `3001` (host) → `3000` (container)
- **File**: `monitoring/docker-compose.yml` → `grafana` service
- **URL**: `http://localhost:3001`
- **Note**: **Intentionally mapped to 3001 on host to avoid conflict with frontend on 3000**
- **Status**: ✅ No conflicts

#### Node Exporter
- **Port**: `9100`
- **File**: `monitoring/docker-compose.yml` → `node-exporter` service
- **Status**: ✅ No conflicts

### Load Testing

#### Gatling
- **Ports Used**: None (client-side tool)
- **Note**: Gatling makes HTTP requests to the backend but doesn't bind to any ports
- **Target**: `http://localhost:8080` (configurable via `-DbaseUrl`)
- **Status**: ✅ No conflicts

## Port Conflict Analysis

### ✅ All Ports Are Unique

No conflicts detected. All services use distinct ports:

1. **80** - Nginx (optional, production only)
2. **3000** - Frontend
3. **3001** - Grafana (host mapping, avoids frontend conflict)
4. **5432** - PostgreSQL
5. **6379** - Redis (optional, cache profile)
6. **8080** - Backend
7. **9090** - Prometheus
8. **9100** - Node Exporter

### Special Considerations

1. **Grafana on 3001**: 
   - Intentionally set to avoid conflict with frontend on 3000
   - Container runs on 3000 internally, mapped to 3001 on host
   - Configuration: `"3001:3000"` in docker-compose.yml

2. **Optional Services**:
   - Redis (6379) - Only with `--profile cache`
   - Nginx (80) - Only with `--profile production`
   - These won't conflict when not running

3. **Load Testing**:
   - Gatling doesn't use ports (client-side)
   - JMeter doesn't use ports (client-side)
   - Both make outbound HTTP requests only

## Verification Commands

### Check if ports are in use:

```bash
# Check all ports
lsof -i -P -n | grep LISTEN

# Check specific ports
lsof -i :8080  # Backend
lsof -i :3000  # Frontend
lsof -i :3001  # Grafana
lsof -i :9090  # Prometheus
lsof -i :5432  # PostgreSQL
lsof -i :9100  # Node Exporter
```

### Using netstat (Linux):

```bash
netstat -tulpn | grep LISTEN
```

### Using ss (Linux):

```bash
ss -tulpn | grep LISTEN
```

## Docker Network Considerations

### Networks Used:

1. **ecommerce-network**: 
   - Used by: Backend, Frontend, PostgreSQL, Redis, Nginx
   - Type: Bridge
   - Created by: `docker-compose.yml`

2. **monitoring**:
   - Used by: Prometheus, Grafana, Node Exporter
   - Type: Bridge
   - Created by: `monitoring/docker-compose.yml`

3. **External Network**:
   - `ecommerce-network` is marked as external in monitoring stack
   - Allows monitoring to access backend metrics

### Internal Container Ports:

Within Docker networks, services communicate using:
- `backend:8080` (not localhost:8080)
- `postgres:5432` (not localhost:5432)
- `prometheus:9090` (not localhost:9090)
- `grafana:3000` (internal, mapped to 3001 on host)

## Troubleshooting Port Conflicts

### If you encounter "port already in use":

1. **Identify the conflicting service**:
   ```bash
   lsof -i :<PORT>
   ```

2. **Stop the conflicting service**:
   ```bash
   # If it's a Docker container
   docker stop <container-name>
   
   # If it's a local process
   kill <PID>
   ```

3. **Change the port** (if needed):
   - Update the port mapping in docker-compose.yml
   - Update any configuration files referencing the port
   - Update documentation

### Common Conflicts:

- **Port 3000**: Often used by other Node.js apps
  - Solution: Grafana uses 3001 (already handled)
  
- **Port 8080**: Common for Java applications
  - Solution: Change in `application.yml` if needed
  
- **Port 5432**: PostgreSQL standard port
  - Solution: Change in docker-compose.yml if you have another PostgreSQL instance

## Port Recommendations for Production

### Development:
- Current port allocation is fine
- All services can run simultaneously

### Production:
- Consider using reverse proxy (Nginx on port 80/443)
- Use environment variables for port configuration
- Consider using non-standard ports for internal services
- Use firewall rules to restrict access

## Environment-Specific Ports

You can override ports using environment variables:

```bash
# Backend port
export SERVER_PORT=8081

# Frontend port
export FRONTEND_PORT=3002

# Grafana port
export GRAFANA_PORT=3002
```

Then update docker-compose.yml accordingly.

## Summary

✅ **No port conflicts detected**

All services are properly configured with unique ports:
- Application services: 80, 3000, 5432, 6379, 8080
- Monitoring services: 3001, 9090, 9100
- Load testing: No ports (client-side only)

The configuration is safe to use as-is.
