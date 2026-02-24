# Nginx Quick Start Guide

## Setup in 3 Steps

### Step 1: Configure Hosts File

#### Linux

**Location**: `/etc/hosts`

**Method 1: Use script (recommended)**
```bash
cd nginx
sudo ./setup-hosts.sh
```

**Method 2: Manual edit**
```bash
sudo nano /etc/hosts
# Add these lines:
127.0.0.1 api.microservices
127.0.0.1 app.microservices
127.0.0.1 monitoring.microservices
```

#### macOS

**Location**: `/etc/hosts`

**Method 1: Use script (recommended)**
```bash
cd nginx
sudo ./setup-hosts.sh
```

**Method 2: Manual edit**
```bash
sudo nano /etc/hosts
# Or use TextEdit:
sudo open -a TextEdit /etc/hosts
# Add these lines:
127.0.0.1 api.microservices
127.0.0.1 app.microservices
127.0.0.1 monitoring.microservices
```

**Flush DNS cache after editing:**
```bash
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder
```

#### Windows

**Location**: `C:\Windows\System32\drivers\etc\hosts`

**Method 1: Notepad (as Administrator)**
1. Press `Win + R`, type `notepad`, press `Ctrl + Shift + Enter`
2. Click "Yes" on UAC prompt
3. File → Open → Navigate to `C:\Windows\System32\drivers\etc\`
4. Change file type to "All Files (*.*)"
5. Open `hosts` file
6. Add these lines at the end:
```
127.0.0.1 api.microservices
127.0.0.1 app.microservices
127.0.0.1 monitoring.microservices
```
7. Save (Ctrl + S)

**Method 2: PowerShell (as Administrator)**
```powershell
notepad C:\Windows\System32\drivers\etc\hosts
# Add the entries above, then save
```

**Flush DNS cache after editing:**
```powershell
ipconfig /flushdns
```

**Note**: See README.md for detailed Windows instructions and troubleshooting.

### Step 2: Start All Services

```bash
# From project root
docker-compose up -d                    # Backend & Frontend
cd monitoring && docker-compose up -d && cd ..  # Monitoring
cd nginx && docker-compose up -d        # Nginx
```

### Step 3: Access Services

- **Frontend**: http://app.microservices
- **Backend API**: http://api.microservices/api
- **Swagger**: http://api.microservices/swagger-ui.html
- **Prometheus**: http://monitoring.microservices/prometheus
- **Grafana**: http://monitoring.microservices/grafana

## Verify Setup

```bash
# Test domains resolve
ping api.microservices
ping app.microservices

# Test Nginx
curl http://api.microservices/actuator/health
curl http://app.microservices

# Check Nginx logs
docker logs ecommerce-nginx
```

## Troubleshooting

**Domain not resolving?**
- Verify hosts file: `cat /etc/hosts | grep microservices`
- Flush DNS cache (macOS): `sudo dscacheutil -flushcache`

**502 Bad Gateway?**
- Check backend is running: `docker ps | grep backend`
- Check network: `docker network ls`

**Configuration errors?**
- Test config: `docker exec ecommerce-nginx nginx -t`
- View logs: `docker logs ecommerce-nginx`
