# Nginx Configuration Guide

Complete guide to Nginx configuration for the E-Commerce microservices platform.

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Virtual Hosts Configuration](#virtual-hosts-configuration)
4. [Reverse Proxy](#reverse-proxy)
5. [Load Balancing](#load-balancing)
6. [Static Website Serving](#static-website-serving)
7. [SSL/TLS Configuration](#ssltls-configuration)
8. [Security Features](#security-features)
9. [Performance Optimization](#performance-optimization)
10. [Troubleshooting](#troubleshooting)

---

## Overview

This Nginx setup provides:
- **Reverse Proxy** for backend API, frontend, and monitoring services
- **Load Balancing** capabilities (ready for scaling)
- **Virtual Hosts** using local domain names
- **Security** headers and rate limiting
- **Performance** optimizations (gzip, caching)

### Architecture

```
Internet/Client
      │
      ▼
   Nginx (Port 80)
      │
      ├──► api.microservices → Backend API (8080)
      ├──► app.microservices → Frontend (3000)
      └──► monitoring.microservices → Prometheus (9090) & Grafana (3000)
```

---

## Integration Note

**Important**: There's an existing Nginx service in the main `docker-compose.yml` (under `production` profile). This Nginx setup in the `nginx/` folder is a more comprehensive configuration with virtual hosts. You can:

1. **Use this Nginx setup** (recommended): Run `cd nginx && docker-compose up -d`
2. **Integrate into main docker-compose**: Copy this configuration to replace the simple nginx service
3. **Use both**: Keep main nginx for production profile, use this for development

## Quick Start

### 1. Configure Hosts File

The hosts file maps domain names to IP addresses. You need to add entries for the microservices domains.

#### Hosts File Locations by Operating System

##### Linux

**Location**: `/etc/hosts`

**How to Edit:**
```bash
# Using nano (user-friendly)
sudo nano /etc/hosts

# Using vim
sudo vim /etc/hosts

# Using gedit (GUI, if available)
sudo gedit /etc/hosts
```

**Quick Add (Command Line):**
```bash
sudo bash -c 'echo -e "127.0.0.1 api.microservices\n127.0.0.1 app.microservices\n127.0.0.1 monitoring.microservices" >> /etc/hosts'
```

**Permissions**: Requires `sudo` or root access

##### macOS

**Location**: `/etc/hosts`

**How to Edit:**
```bash
# Using nano (recommended)
sudo nano /etc/hosts

# Using vim
sudo vim /etc/hosts

# Using TextEdit (GUI)
sudo open -a TextEdit /etc/hosts
```

**Quick Add (Command Line):**
```bash
sudo bash -c 'echo -e "127.0.0.1 api.microservices\n127.0.0.1 app.microservices\n127.0.0.1 monitoring.microservices" >> /etc/hosts'
```

**Permissions**: Requires `sudo` or administrator password

**Note**: macOS may require you to unlock System Preferences first:
1. System Preferences → Security & Privacy → Click the lock icon
2. Enter your password
3. Then edit the hosts file

##### Windows

**Location**: `C:\Windows\System32\drivers\etc\hosts`

**How to Edit:**

**Method 1: Notepad (as Administrator)**
1. Press `Win + R` to open Run dialog
2. Type: `notepad`
3. Press `Ctrl + Shift + Enter` (runs as Administrator)
4. Click "Yes" on UAC prompt
5. In Notepad: File → Open
6. Navigate to: `C:\Windows\System32\drivers\etc\`
7. Change file type filter to "All Files (*.*)"
8. Select `hosts` file and open
9. Add entries at the end of the file
10. Save (Ctrl + S)

**Method 2: PowerShell (as Administrator)**
```powershell
# Open PowerShell as Administrator, then:
notepad C:\Windows\System32\drivers\etc\hosts
```

**Method 3: Command Prompt (as Administrator)**
```cmd
# Open CMD as Administrator, then:
notepad C:\Windows\System32\drivers\etc\hosts
```

**Method 4: Using VS Code (as Administrator)**
```powershell
# Open PowerShell as Administrator
code C:\Windows\System32\drivers\etc\hosts
```

**Quick Add (PowerShell as Administrator):**
```powershell
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "`n127.0.0.1 api.microservices"
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "127.0.0.1 app.microservices"
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "127.0.0.1 monitoring.microservices"
```

**Permissions**: Requires Administrator privileges

**Note**: Windows may hide the `hosts` file. To show it:
- File Explorer → View → Check "Hidden items"
- Or use the command line methods above

#### Entries to Add

Add these lines to your hosts file:

```
127.0.0.1 api.microservices
127.0.0.1 app.microservices
127.0.0.1 monitoring.microservices
```

**Format**: `IP_ADDRESS DOMAIN_NAME`

- `127.0.0.1` = localhost (your local machine)
- Each domain on a separate line
- No `http://` or `https://` prefix
- Comments start with `#`

**Example hosts file:**
```
# Localhost entries
127.0.0.1       localhost
::1             localhost

# E-Commerce Microservices
127.0.0.1       api.microservices
127.0.0.1       app.microservices
127.0.0.1       monitoring.microservices
```

#### Verify Configuration

**Linux/macOS:**
```bash
# Test DNS resolution
ping api.microservices
ping app.microservices
ping monitoring.microservices

# Or use nslookup
nslookup api.microservices

# Or use dig
dig api.microservices
```

**Windows:**
```powershell
# Test DNS resolution
ping api.microservices
ping app.microservices
ping monitoring.microservices

# Or use nslookup
nslookup api.microservices
```

**Expected Output:**
```
PING api.microservices (127.0.0.1): 56 data bytes
64 bytes from 127.0.0.1: icmp_seq=0 ttl=64 time=0.025 ms
```

#### Troubleshooting Hosts File

**Issue: Changes not taking effect**

**Linux/macOS:**
```bash
# Flush DNS cache
sudo systemd-resolve --flush-caches  # systemd-based Linux
sudo dscacheutil -flushcache        # macOS
sudo killall -HUP mDNSResponder     # macOS (alternative)
```

**Windows:**
```powershell
# Flush DNS cache
ipconfig /flushdns
```

**Issue: Permission denied**

- **Linux/macOS**: Use `sudo` command
- **Windows**: Run editor as Administrator

**Issue: File not found**

- **Windows**: The file might be hidden. Use command line or enable "Show hidden files"
- **Linux/macOS**: File should exist by default. If missing, create it: `sudo touch /etc/hosts`

**Issue: Domains still not resolving**

1. Verify entries are correct (no typos)
2. Ensure no extra spaces
3. Flush DNS cache (see above)
4. Restart browser/application
5. Check for duplicate entries
6. Verify file was saved correctly

### 2. Start Services

```bash
# Start backend, frontend, and monitoring
cd ..
docker-compose up -d
cd monitoring && docker-compose up -d && cd ..

# Start Nginx
cd nginx
docker-compose up -d
```

### 3. Access Services

- **Frontend**: http://app.microservices
- **Backend API**: http://api.microservices/api
- **Swagger UI**: http://api.microservices/swagger-ui.html
- **Prometheus**: http://monitoring.microservices/prometheus
- **Grafana**: http://monitoring.microservices/grafana

---

## Virtual Hosts Configuration

### Current Virtual Hosts

| Domain | Service | Configuration File |
|--------|---------|-------------------|
| `api.microservices` | Backend API | `conf.d/api.microservices.conf` |
| `app.microservices` | Frontend App | `conf.d/app.microservices.conf` |
| `monitoring.microservices` | Monitoring | `conf.d/monitoring.microservices.conf` |

### How Virtual Hosts Work

Nginx uses the `server_name` directive to match incoming requests:

```nginx
server {
    listen 80;
    server_name api.microservices;  # Matches this domain
    
    location / {
        proxy_pass http://backend_api;
    }
}
```

### Adding a New Virtual Host

1. **Create configuration file**: `conf.d/newservice.microservices.conf`

```nginx
server {
    listen 80;
    server_name newservice.microservices;
    
    location / {
        proxy_pass http://upstream_name;
    }
}
```

2. **Add to hosts file**: `127.0.0.1 newservice.microservices`

3. **Reload Nginx**: `docker exec ecommerce-nginx nginx -s reload`

---

## Reverse Proxy

### What is Reverse Proxy?

A reverse proxy sits between clients and backend servers, forwarding requests and responses. Benefits:
- **Single entry point** for multiple services
- **SSL termination** (HTTPS at Nginx, HTTP to backends)
- **Load balancing**
- **Caching**
- **Security** (hide backend structure)

### Current Reverse Proxy Setup

#### Backend API Proxy

```nginx
location /api {
    proxy_pass http://backend_api;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

**Access**: `http://api.microservices/api/products`

#### Frontend Proxy

```nginx
location / {
    proxy_pass http://frontend_app;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
}
```

**Access**: `http://app.microservices`

### Proxy Headers Explained

- **Host**: Original host header from client
- **X-Real-IP**: Client's real IP address
- **X-Forwarded-For**: Chain of proxy IPs
- **X-Forwarded-Proto**: Original protocol (http/https)
- **X-Forwarded-Host**: Original host

### WebSocket Support

For WebSocket connections (Grafana, real-time apps):

```nginx
location /ws {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

---

## Load Balancing

### Overview

Nginx can distribute requests across multiple backend servers for:
- **High availability**
- **Performance scaling**
- **Fault tolerance**

### Load Balancing Methods

#### 1. Round Robin (Default)

```nginx
upstream backend_api {
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
}
```

Requests distributed sequentially: 1→2→3→1→2→3...

#### 2. Least Connections

```nginx
upstream backend_api {
    least_conn;
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
}
```

Sends request to server with fewest active connections.

#### 3. IP Hash (Session Persistence)

```nginx
upstream backend_api {
    ip_hash;
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
}
```

Same client IP always goes to same server (useful for sessions).

#### 4. Weighted Round Robin

```nginx
upstream backend_api {
    server backend1:8080 weight=3;
    server backend2:8080 weight=2;
    server backend3:8080 weight=1;
}
```

Distributes 3:2:1 ratio (backend1 gets 50%, backend2 gets 33%, backend3 gets 17%).

### Health Checks

```nginx
upstream backend_api {
    server backend1:8080 max_fails=3 fail_timeout=30s;
    server backend2:8080 max_fails=3 fail_timeout=30s;
}
```

- **max_fails**: Number of failures before marking server down
- **fail_timeout**: Time before retrying failed server

### Active Health Checks (Nginx Plus)

For Nginx Plus (commercial), you can use active health checks:

```nginx
upstream backend_api {
    zone backend_api 64k;
    server backend1:8080;
    server backend2:8080;
}

match server_ok {
    status 200-399;
    header Content-Type ~ "application/json";
}

server {
    location / {
        proxy_pass http://backend_api;
        health_check match=server_ok interval=5s fails=2 passes=3;
    }
}
```

### Example: Scaling Backend

To add more backend instances:

1. **Update upstream** (`conf.d/upstreams.conf`):
```nginx
upstream backend_api {
    least_conn;
    server backend:8080;
    server backend2:8080;  # Add new instance
    server backend3:8080;  # Add new instance
}
```

2. **Start additional backend containers**:
```bash
docker run -d --name backend2 -p 8081:8080 backend-image
docker run -d --name backend3 -p 8082:8080 backend-image
```

3. **Reload Nginx**: `docker exec ecommerce-nginx nginx -s reload`

---

## Static Website Serving

### Basic Static File Serving

```nginx
server {
    listen 80;
    server_name static.microservices;
    root /usr/share/nginx/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### Static File Caching

```nginx
location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
    access_log off;
}
```

**Cache durations:**
- `1y` = 1 year
- `1M` = 1 month
- `1w` = 1 week
- `1d` = 1 day
- `1h` = 1 hour

### Gzip Compression for Static Files

Already enabled in main `nginx.conf`:

```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript;
gzip_comp_level 6;
```

### Serving Multiple Static Sites

```nginx
# Site 1
server {
    server_name site1.microservices;
    root /usr/share/nginx/html/site1;
}

# Site 2
server {
    server_name site2.microservices;
    root /usr/share/nginx/html/site2;
}
```

---

## SSL/TLS Configuration

### Basic HTTPS Setup

```nginx
server {
    listen 443 ssl http2;
    server_name api.microservices;
    
    ssl_certificate /etc/nginx/ssl/api.microservices.crt;
    ssl_certificate_key /etc/nginx/ssl/api.microservices.key;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    location / {
        proxy_pass http://backend_api;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name api.microservices;
    return 301 https://$server_name$request_uri;
}
```

### Self-Signed Certificate (Development)

```bash
# Generate self-signed certificate
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout nginx/ssl/api.microservices.key \
    -out nginx/ssl/api.microservices.crt \
    -subj "/CN=api.microservices"
```

### Let's Encrypt (Production)

Use Certbot:

```bash
certbot --nginx -d api.microservices -d app.microservices
```

---

## Security Features

### 1. Rate Limiting

Prevents abuse and DDoS attacks:

```nginx
# Define rate limit zone
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

# Apply to location
location /api {
    limit_req zone=api_limit burst=20 nodelay;
    proxy_pass http://backend_api;
}
```

**Parameters:**
- `10m`: 10MB memory for storing IP addresses
- `rate=10r/s`: 10 requests per second
- `burst=20`: Allow 20 requests in burst
- `nodelay`: Don't delay burst requests

### 2. Security Headers

```nginx
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Strict-Transport-Security "max-age=31536000" always;
```

### 3. Hide Nginx Version

```nginx
server_tokens off;
```

### 4. Basic Authentication

```nginx
location /admin {
    auth_basic "Admin Area";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_pass http://backend_api;
}
```

**Create password file:**
```bash
htpasswd -c nginx/.htpasswd admin
```

### 5. IP Whitelisting

```nginx
location /admin {
    allow 192.168.1.0/24;
    allow 10.0.0.0/8;
    deny all;
    proxy_pass http://backend_api;
}
```

### 6. Request Size Limits

```nginx
client_max_body_size 20M;  # In http block
```

---

## Performance Optimization

### 1. Gzip Compression

Already configured in `nginx.conf`:

```nginx
gzip on;
gzip_vary on;
gzip_comp_level 6;
gzip_types text/plain text/css application/json;
```

### 2. Caching

#### Proxy Caching

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=1g;

server {
    location /api {
        proxy_cache api_cache;
        proxy_cache_valid 200 302 10m;
        proxy_cache_valid 404 1m;
        proxy_pass http://backend_api;
    }
}
```

#### Browser Caching

```nginx
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### 3. Keep-Alive Connections

```nginx
upstream backend_api {
    keepalive 32;  # Maintain 32 connections
    server backend:8080;
}
```

### 4. Worker Processes

```nginx
worker_processes auto;  # Auto-detect CPU cores
worker_connections 1024;
```

### 5. File Descriptors

Increase system limits:
```bash
ulimit -n 65535
```

---

## Advanced Use Cases

### 1. URL Rewriting

```nginx
# Remove /api prefix
location /api {
    rewrite ^/api/(.*) /$1 break;
    proxy_pass http://backend_api;
}

# Add prefix
location /v1 {
    rewrite ^/v1/(.*) /api/v1/$1 break;
    proxy_pass http://backend_api;
}
```

### 2. Request Routing Based on Headers

```nginx
location / {
    if ($http_user_agent ~* "Mobile") {
        proxy_pass http://mobile_backend;
    }
    proxy_pass http://desktop_backend;
}
```

### 3. A/B Testing

```nginx
split_clients "${remote_addr}${http_user_agent}" $variant {
    50% backend_v1;
    50% backend_v2;
}

location / {
    proxy_pass http://$variant;
}
```

### 4. Maintenance Mode

```nginx
location / {
    if (-f /etc/nginx/maintenance.flag) {
        return 503;
    }
    proxy_pass http://backend_api;
}

error_page 503 @maintenance;
location @maintenance {
    root /usr/share/nginx/html;
    rewrite ^(.*)$ /maintenance.html break;
}
```

### 5. Logging to Different Files

```nginx
access_log /var/log/nginx/api.access.log main;
error_log /var/log/nginx/api.error.log warn;
```

### 6. Custom Error Pages

```nginx
error_page 404 /404.html;
error_page 500 502 503 504 /50x.html;

location = /50x.html {
    root /usr/share/nginx/html;
}
```

---

## Configuration Files Structure

```
nginx/
├── nginx.conf                 # Main configuration
├── conf.d/
│   ├── upstreams.conf        # Upstream/load balancing definitions
│   ├── api.microservices.conf # Backend API virtual host
│   ├── app.microservices.conf # Frontend virtual host
│   ├── monitoring.microservices.conf # Monitoring virtual host
│   └── default.conf          # Default/catch-all server
├── logs/                      # Log files (created automatically)
├── static/                    # Static files (optional)
├── ssl/                       # SSL certificates (optional)
├── docker-compose.yml         # Docker setup
└── README.md                  # This file
```

---

## Troubleshooting

### Check Nginx Configuration

```bash
# Test configuration syntax
docker exec ecommerce-nginx nginx -t

# View configuration
docker exec ecommerce-nginx nginx -T
```

### Reload Configuration

```bash
# Reload without downtime
docker exec ecommerce-nginx nginx -s reload

# Restart container
docker restart ecommerce-nginx
```

### View Logs

```bash
# Access logs
docker exec ecommerce-nginx tail -f /var/log/nginx/access.log

# Error logs
docker exec ecommerce-nginx tail -f /var/log/nginx/error.log

# Specific virtual host logs
docker exec ecommerce-nginx tail -f /var/log/nginx/api.error.log
```

### Common Issues

#### 1. 502 Bad Gateway

**Cause**: Backend server is down or unreachable

**Solution**:
```bash
# Check backend is running
docker ps | grep backend

# Check backend logs
docker logs ecommerce-backend

# Verify network connectivity
docker exec ecommerce-nginx ping backend
```

#### 2. 404 Not Found

**Cause**: Incorrect proxy_pass URL or location block

**Solution**: Check `proxy_pass` directive matches upstream name

#### 3. Domain Not Resolving

**Cause**: Hosts file not configured

**Solution**: Verify hosts file entries:
```bash
cat /etc/hosts | grep microservices
```

#### 4. Connection Refused

**Cause**: Service not listening on expected port

**Solution**: Verify service ports:
```bash
docker ps
netstat -tulpn | grep LISTEN
```

### Debug Mode

Enable debug logging:

```nginx
error_log /var/log/nginx/error.log debug;
```

---

## Best Practices

1. **Use upstream blocks** for load balancing
2. **Set appropriate timeouts** for your application
3. **Enable gzip** for text-based content
4. **Use rate limiting** to prevent abuse
5. **Set security headers** for all responses
6. **Cache static assets** aggressively
7. **Monitor access and error logs** regularly
8. **Test configuration** before reloading: `nginx -t`
9. **Use separate log files** per virtual host
10. **Keep Nginx updated** for security patches

---

## Production Checklist

- [ ] SSL/TLS certificates configured
- [ ] HTTP to HTTPS redirect enabled
- [ ] Security headers set
- [ ] Rate limiting configured
- [ ] Log rotation set up
- [ ] Monitoring and alerting configured
- [ ] Backup configuration files
- [ ] Load testing performed
- [ ] Error pages customized
- [ ] Maintenance mode script ready

---

## Resources

- [Nginx Official Documentation](https://nginx.org/en/docs/)
- [Nginx Beginner's Guide](https://nginx.org/en/docs/beginners_guide.html)
- [Nginx Admin's Guide](https://nginx.org/en/docs/admin_guide.html)
- [Nginx Configuration Examples](https://www.nginx.com/resources/wiki/start/topics/examples/)

---

## Support

For issues:
1. Check logs: `docker logs ecommerce-nginx`
2. Test configuration: `docker exec ecommerce-nginx nginx -t`
3. Verify hosts file entries
4. Check service connectivity
5. Review this README for common solutions
