# Nginx Integration Guide

## Integration Options

You have multiple options for integrating this Nginx setup:

### Option 1: Standalone Nginx (Recommended for Development)

Use the Nginx folder's docker-compose independently:

```bash
cd nginx
docker-compose up -d
```

**Pros:**
- Independent management
- Easy to start/stop separately
- Clear separation of concerns

**Cons:**
- Need to manage networks separately
- Extra docker-compose file

### Option 2: Integrate into Main docker-compose.yml

Replace the existing nginx service in root `docker-compose.yml`:

1. **Remove** the existing nginx service (lines 79-94)
2. **Add** this configuration:

```yaml
nginx:
  image: nginx:alpine
  container_name: ecommerce-nginx
  ports:
    - "80:80"
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    - ./nginx/conf.d:/etc/nginx/conf.d:ro
    - ./nginx/logs:/var/log/nginx
    - ./nginx/static:/usr/share/nginx/html:ro
  networks:
    - ecommerce-network
    - monitoring
  depends_on:
    - backend
    - frontend
  restart: unless-stopped
```

3. **Add monitoring network** to main docker-compose:

```yaml
networks:
  ecommerce-network:
    driver: bridge
  monitoring:
    external: true
```

**Pros:**
- Single docker-compose file
- Easier to manage all services together

**Cons:**
- Less modular
- Harder to update Nginx independently

### Option 3: Use Both (Production Profile)

Keep the simple nginx in main docker-compose for production, use this for development.

## Network Configuration

Ensure networks are created:

```bash
# Create networks if they don't exist
docker network create ecommerce-network 2>/dev/null || true
docker network create monitoring 2>/dev/null || true
```

## Service Dependencies

Nginx depends on:
- **Backend**: Must be running for API routes
- **Frontend**: Must be running for app routes
- **Prometheus**: Must be running for monitoring routes
- **Grafana**: Must be running for monitoring routes

Start order:
1. Backend & Frontend: `docker-compose up -d`
2. Monitoring: `cd monitoring && docker-compose up -d`
3. Nginx: `cd nginx && docker-compose up -d`

## Verification

After starting all services:

```bash
# Test API
curl http://api.microservices/api/products

# Test Frontend
curl http://app.microservices

# Test Monitoring
curl http://monitoring.microservices/grafana
```
