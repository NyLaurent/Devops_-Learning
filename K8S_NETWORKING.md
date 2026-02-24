# Kubernetes Networking Explained - E-Commerce Platform

This comprehensive guide explains Kubernetes networking concepts in detail, using our E-Commerce Platform as a practical example. Understanding K8s networking is crucial for deploying, scaling, and securing containerized applications.

## 📚 Table of Contents

1. [Core Networking Concepts](#core-networking-concepts)
2. [Pod Networking](#pod-networking)
3. [Services](#services)
4. [Ingress & Ingress Controllers](#ingress--ingress-controllers)
5. [Network Policies](#network-policies)
6. [DNS & Service Discovery](#dns--service-discovery)
7. [Our Platform's Network Architecture](#our-platforms-network-architecture)
8. [Troubleshooting](#troubleshooting)
9. [Comparison: Docker vs Kubernetes Networking](#comparison-docker-vs-kubernetes-networking)

---

## Core Networking Concepts

### The Kubernetes Network Model

Kubernetes follows a **flat network model** where:

1. **Every Pod gets its own IP address** - Pods can communicate directly without NAT
2. **Pods can reach all other Pods** - Without network address translation
3. **Agents on nodes can communicate with Pods** - Node-to-Pod communication
4. **Pods see themselves as others see them** - No IP masquerading

### Key Principles

- **IP-per-Pod**: Each pod has a unique IP in the cluster network
- **No NAT between Pods**: Direct communication without translation
- **Service Abstraction**: Services provide stable endpoints for dynamic pods
- **Network Plugins**: CNI (Container Network Interface) plugins implement networking

### In Our Platform

```
┌─────────────────────────────────────────────────────────┐
│  Kubernetes Cluster Network (e.g., 10.244.0.0/16)     │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ Backend Pod  │  │ Frontend Pod │  │ Postgres Pod│ │
│  │ 10.244.1.5   │  │ 10.244.1.6   │  │ 10.244.1.7   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│         │                  │                  │        │
│         └──────────────────┼──────────────────┘        │
│                            │                            │
│                    ┌───────▼────────┐                   │
│                    │  Services      │                   │
│                    │  (Virtual IPs) │                   │
│                    └────────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

---

## Pod Networking

### What is a Pod Network?

A **Pod network** is the IP address space assigned to pods. Each pod receives:
- **One IP address** from the cluster's Pod CIDR
- **Full network namespace** (can listen on any port)
- **Direct routing** to other pods

### Pod IP Assignment

When a pod is created:
1. **CNI Plugin** assigns an IP from the node's Pod CIDR
2. **IP is unique** across the entire cluster
3. **IP persists** until pod is deleted
4. **New pod = new IP** (even if same deployment)

### Example from Our Platform

```yaml
# backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: backend
        ports:
        - containerPort: 8080  # Pod listens on this port
```

**What happens:**
- Pod 1: `10.244.1.10:8080` (assigned by CNI)
- Pod 2: `10.244.2.15:8080` (assigned by CNI)
- Both pods have different IPs but same port

### Pod-to-Pod Communication

**Direct Communication:**
```bash
# Pod 1 can directly reach Pod 2
curl http://10.244.2.15:8080/api/products
```

**Problems with Direct IPs:**
- ❌ IPs change when pods restart
- ❌ Need to discover pod IPs
- ❌ No load balancing
- ❌ Hard to manage

**Solution: Services** (covered next)

---

## Services

Services provide **stable network endpoints** for pods. They solve the problem of dynamic pod IPs.

### Service Types

#### 1. ClusterIP (Default)

**What it does:**
- Creates a **virtual IP** inside the cluster
- **Load balances** traffic to pod endpoints
- **Only accessible** from within the cluster
- **DNS name** for service discovery

**In Our Platform:**

```yaml
# backend/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  type: ClusterIP  # Default, can be omitted
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: backend
```

**How it works:**
1. Service gets virtual IP: `10.96.0.1` (example)
2. Endpoints controller watches pods with `app: backend`
3. Creates Endpoints object with pod IPs: `10.244.1.10:8080`, `10.244.2.15:8080`
4. kube-proxy creates iptables rules to route traffic
5. DNS entry: `backend-service.ecommerce.svc.cluster.local`

**Traffic Flow:**
```
Frontend Pod → backend-service:8080
              ↓
         kube-proxy (iptables)
              ↓
    ┌─────────┴─────────┐
    ↓                   ↓
Backend Pod 1    Backend Pod 2
10.244.1.10:8080  10.244.2.15:8080
```

**Our Services:**
- `backend-service` (ClusterIP) - Internal API access
- `frontend-service` (ClusterIP) - Internal frontend access
- `postgres-service` (Headless) - Direct database access
- `redis-service` (ClusterIP) - Cache access

#### 2. Headless Service (ClusterIP: None)

**What it does:**
- **No virtual IP** assigned
- Returns **all pod IPs** directly
- Used for **StatefulSets** or direct pod access
- Enables **DNS-based service discovery**

**In Our Platform:**

```yaml
# postgres/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
spec:
  clusterIP: None  # Headless service
  ports:
  - port: 5432
  selector:
    app: postgres
```

**Why Headless for PostgreSQL:**
- StatefulSet needs stable network identity
- Direct pod-to-pod communication
- DNS returns pod IP: `postgres-0.postgres-service.ecommerce.svc.cluster.local`

**DNS Resolution:**
```bash
# Returns pod IP directly
nslookup postgres-service.ecommerce.svc.cluster.local
# Returns: 10.244.1.7

# StatefulSet pod DNS
nslookup postgres-0.postgres-service.ecommerce.svc.cluster.local
# Returns: 10.244.1.7
```

#### 3. NodePort

**What it does:**
- Exposes service on **each node's IP** at a static port
- Port range: **30000-32767**
- Accessible from **outside the cluster**

**Example (not in our platform, but useful for demos):**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-nodeport
spec:
  type: NodePort
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30080  # Optional, auto-assigned if omitted
  selector:
    app: backend
```

**Access:**
```bash
# Access from outside cluster
curl http://<any-node-ip>:30080/api/products
```

**Use Cases:**
- Development/testing
- Legacy applications
- Direct node access needed

#### 4. LoadBalancer

**What it does:**
- Exposes service externally using **cloud provider's load balancer**
- Creates external IP/URL
- **Cloud-specific** (AWS ELB, GCP Load Balancer, Azure LB)

**Example:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend-lb
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 3000
  selector:
    app: frontend
```

**Cloud Behavior:**
- **AWS**: Creates ELB, assigns DNS name
- **GCP**: Creates Network Load Balancer, assigns IP
- **Azure**: Creates Azure Load Balancer, assigns IP

**In Our Platform:**
We use **Ingress** instead (better for HTTP/HTTPS)

---

## Ingress & Ingress Controllers

### What is Ingress?

**Ingress** provides HTTP/HTTPS routing to services based on:
- **Hostname** (e.g., `api.ecommerce.com`)
- **Path** (e.g., `/api/*`)
- **TLS termination**

### Ingress vs Service

| Feature | Service | Ingress |
|---------|---------|---------|
| Layer | L4 (TCP/UDP) | L7 (HTTP/HTTPS) |
| Routing | Port-based | Path/Host-based |
| TLS | Not supported | Supported |
| Load Balancing | Yes | Yes (via controller) |

### Ingress Controller

**Ingress Controller** is a **pod** that:
- Watches Ingress resources
- Implements routing rules
- Handles TLS termination
- Provides load balancing

**Popular Controllers:**
- **NGINX Ingress Controller** (most common)
- **Traefik**
- **HAProxy**
- **Istio Gateway**

### Our Ingress Configuration

```yaml
# ingress/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: ecommerce.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 3000
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 8080
```

### How It Works

```
Internet Request
    ↓
Ingress Controller (nginx pod)
    ↓
Routing Rules (from Ingress resource)
    ↓
    ├─ / → frontend-service:3000
    └─ /api → backend-service:8080
```

### Path Types

1. **Exact**: Matches exactly `/api/products`
2. **Prefix**: Matches `/api/*` (our use case)
3. **ImplementationSpecific**: Controller-specific behavior

### TLS/HTTPS

```yaml
spec:
  tls:
  - hosts:
    - ecommerce.local
    secretName: ecommerce-tls
```

**TLS Secret:**
```bash
kubectl create secret tls ecommerce-tls \
  --cert=tls.crt \
  --key=tls.key \
  -n ecommerce
```

**With cert-manager (automatic):**
```yaml
annotations:
  cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

### Installing Ingress Controller

**NGINX Ingress:**
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
```

**For minikube:**
```bash
minikube addons enable ingress
```

---

## Network Policies

### What are Network Policies?

**Network Policies** are **firewall rules** for pods. They control:
- **Ingress** (incoming traffic)
- **Egress** (outgoing traffic)

### Default Behavior

- **No Network Policy = Allow All**
- **Network Policy exists = Deny All** (unless explicitly allowed)

### Our Network Policies

```yaml
# base/network-policy.yaml

# 1. Default Deny All
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
spec:
  podSelector: {}  # Applies to all pods
  policyTypes:
  - Ingress
  - Egress
```

**Effect:** All pods are isolated by default.

```yaml
# 2. Frontend Policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: frontend-policy
spec:
  podSelector:
    matchLabels:
      app: frontend
  ingress:
  - from:
    - namespaceSelector: {}  # Allow from any namespace
    ports:
    - protocol: TCP
      port: 3000
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: backend
    ports:
    - protocol: TCP
      port: 8080
  - to:
    - namespaceSelector: {}  # DNS
    ports:
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53
```

**What this allows:**
- ✅ Ingress: Anyone can access frontend on port 3000
- ✅ Egress: Frontend can reach backend on port 8080
- ✅ Egress: Frontend can reach DNS (port 53)

```yaml
# 3. Backend Policy
spec:
  podSelector:
    matchLabels:
      app: backend
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
```

**What this allows:**
- ✅ Ingress: Only frontend can reach backend
- ✅ Egress: Backend can reach PostgreSQL
- ✅ Egress: Backend can reach Redis

### Network Policy Selectors

**Pod Selector:**
```yaml
podSelector:
  matchLabels:
    app: backend
```

**Namespace Selector:**
```yaml
from:
  - namespaceSelector:
      matchLabels:
        name: ecommerce
```

**IP Block:**
```yaml
from:
  - ipBlock:
      cidr: 10.244.0.0/16
      except:
      - 10.244.1.0/24
```

### Traffic Flow in Our Platform

```
Internet
  ↓
Ingress Controller (bypasses Network Policies)
  ↓
Frontend Pod (allowed by frontend-policy)
  ↓
Backend Pod (allowed by backend-policy)
  ↓
PostgreSQL Pod (allowed by postgres-policy)
```

---

## DNS & Service Discovery

### Kubernetes DNS

Kubernetes has a **built-in DNS server** (CoreDNS) that provides:
- **Service discovery** via DNS names
- **Automatic DNS entries** for services
- **Pod DNS resolution**

### DNS Naming Convention

**Service DNS:**
```
<service-name>.<namespace>.svc.cluster.local
```

**Examples from Our Platform:**
```
backend-service.ecommerce.svc.cluster.local
frontend-service.ecommerce.svc.cluster.local
postgres-service.ecommerce.svc.cluster.local
redis-service.ecommerce.svc.cluster.local
```

**Short Names:**
```
backend-service        # Same namespace
backend-service.ecommerce  # Cross-namespace
```

### StatefulSet DNS

**Headless Service + StatefulSet:**
```
<statefulset-name>-<ordinal>.<service-name>.<namespace>.svc.cluster.local
```

**Our PostgreSQL:**
```
postgres-0.postgres-service.ecommerce.svc.cluster.local
```

### DNS Resolution in Pods

**From Backend Pod:**
```bash
# Resolve PostgreSQL
nslookup postgres-service
# Returns: 10.96.0.5 (ClusterIP) or pod IPs (headless)

# Resolve Redis
nslookup redis-service
# Returns: 10.96.0.6

# Resolve Frontend
nslookup frontend-service
# Returns: 10.96.0.7
```

**In Application Code:**
```java
// Spring Boot application.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres-service:5432/ecommerce
    # Uses DNS to resolve postgres-service
```

### DNS Records

**A Record (Service IP):**
```
backend-service.ecommerce.svc.cluster.local → 10.96.0.1
```

**SRV Record (Port info):**
```
_backend._tcp.backend-service.ecommerce.svc.cluster.local
```

**Pod DNS (Headless):**
```
postgres-0.postgres-service.ecommerce.svc.cluster.local → 10.244.1.7
```

---

## Our Platform's Network Architecture

### Complete Network Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Internet/External                        │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ↓
        ┌───────────────────────────────┐
        │   Ingress Controller          │
        │   (nginx-ingress pod)         │
        │   IP: 10.244.0.10             │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
        ↓                               ↓
┌───────────────┐              ┌─────────────────┐
│ Ingress Rules │              │  Ingress Rules  │
│ Path: /       │              │  Path: /api     │
└───────┬───────┘              └────────┬────────┘
        │                               │
        ↓                               ↓
┌───────────────┐              ┌─────────────────┐
│ frontend-     │              │ backend-service │
│ service       │              │ (ClusterIP)     │
│ (ClusterIP)   │              │ 10.96.0.1:8080  │
│ 10.96.0.2:3000│              └────────┬────────┘
└───────┬───────┘                       │
        │                               │
        │         ┌─────────────────────┘
        │         │
        ↓         ↓
┌───────────────┐  ┌─────────────────┐
│ Frontend Pod 1│  │ Backend Pod 1   │
│ 10.244.1.5    │  │ 10.244.1.10     │
└───────────────┘  └────────┬────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ↓                   ↓                   ↓
┌───────────────┐  ┌─────────────────┐  ┌──────────────┐
│ Frontend Pod 2│  │ Backend Pod 2   │  │ PostgreSQL   │
│ 10.244.2.6    │  │ 10.244.2.15    │  │ Pod          │
└───────────────┘  └─────────────────┘  │ 10.244.1.7   │
                                        └──────┬───────┘
                                               │
                                        ┌──────▼───────┐
                                        │ postgres-    │
                                        │ service      │
                                        │ (Headless)   │
                                        └──────────────┘
```

### Service Endpoints

**Backend Service Endpoints:**
```bash
kubectl get endpoints backend-service -n ecommerce

NAME              ENDPOINTS
backend-service   10.244.1.10:8080,10.244.2.15:8080
```

**How kube-proxy routes:**
1. Request to `backend-service:8080`
2. kube-proxy (iptables) selects pod (round-robin)
3. Forwards to `10.244.1.10:8080` or `10.244.2.15:8080`

### Network Policies Flow

```
┌─────────────────────────────────────────────────┐
│  Network Policy Enforcement                    │
└─────────────────────────────────────────────────┘

Internet → Ingress (bypass) → Frontend Pod
                                    ↓
                          (frontend-policy allows)
                                    ↓
                            Backend Pod
                                    ↓
                          (backend-policy allows)
                                    ↓
                            PostgreSQL Pod
```

---

## Troubleshooting

### Common Issues & Solutions

#### 1. Pods Can't Communicate

**Symptoms:**
- Backend can't reach PostgreSQL
- Frontend can't reach backend

**Debug Steps:**
```bash
# Check pod IPs
kubectl get pods -o wide -n ecommerce

# Test connectivity from pod
kubectl exec -it backend-xxx -- ping postgres-service

# Check DNS resolution
kubectl exec -it backend-xxx -- nslookup postgres-service

# Check network policies
kubectl get networkpolicies -n ecommerce
kubectl describe networkpolicy backend-policy -n ecommerce
```

**Common Causes:**
- Network Policy blocking traffic
- Service selector doesn't match pod labels
- DNS not resolving

#### 2. Service Not Accessible

**Symptoms:**
- `curl backend-service:8080` fails
- Service exists but no endpoints

**Debug Steps:**
```bash
# Check service
kubectl get svc backend-service -n ecommerce

# Check endpoints
kubectl get endpoints backend-service -n ecommerce

# If no endpoints, check pod labels
kubectl get pods --show-labels -n ecommerce

# Verify service selector matches pod labels
kubectl describe svc backend-service -n ecommerce
```

**Common Causes:**
- Service selector doesn't match pod labels
- No pods running
- Pods not ready (readiness probe failing)

#### 3. Ingress Not Working

**Symptoms:**
- Can't access via Ingress URL
- 404 or connection refused

**Debug Steps:**
```bash
# Check Ingress
kubectl get ingress -n ecommerce
kubectl describe ingress ecommerce-ingress -n ecommerce

# Check Ingress Controller
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx <ingress-controller-pod>

# Test service directly
kubectl port-forward svc/frontend-service 3000:3000 -n ecommerce
curl http://localhost:3000
```

**Common Causes:**
- Ingress Controller not installed
- Wrong ingressClassName
- Service not found
- Path routing incorrect

#### 4. DNS Resolution Fails

**Symptoms:**
- `nslookup backend-service` fails
- Application can't resolve service names

**Debug Steps:**
```bash
# Check CoreDNS
kubectl get pods -n kube-system | grep coredns
kubectl logs -n kube-system <coredns-pod>

# Test DNS from pod
kubectl exec -it backend-xxx -- nslookup backend-service

# Check /etc/resolv.conf in pod
kubectl exec -it backend-xxx -- cat /etc/resolv.conf
```

**Common Causes:**
- CoreDNS not running
- DNS service not configured
- Network issues

#### 5. Network Policy Blocking Traffic

**Symptoms:**
- Pods can't communicate despite services existing
- Traffic works without Network Policies

**Debug Steps:**
```bash
# List all Network Policies
kubectl get networkpolicies -n ecommerce

# Describe specific policy
kubectl describe networkpolicy backend-policy -n ecommerce

# Temporarily remove policy to test
kubectl delete networkpolicy backend-policy -n ecommerce

# Check if traffic works, then add policy back with correct rules
```

**Common Causes:**
- Default deny-all policy too restrictive
- Missing egress rules for DNS (port 53)
- Selector labels don't match

### Useful Commands

```bash
# View all network resources
kubectl get svc,ingress,endpoints,networkpolicies -n ecommerce

# Port forward for testing
kubectl port-forward svc/backend-service 8080:8080 -n ecommerce

# Exec into pod for network testing
kubectl exec -it <pod-name> -n ecommerce -- /bin/sh

# Test connectivity
kubectl exec -it backend-xxx -- wget -O- http://postgres-service:5432

# View iptables rules (on node)
sudo iptables -t nat -L | grep backend-service

# Check service endpoints
kubectl get endpoints -n ecommerce

# DNS testing
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup backend-service.ecommerce
```

---

## Comparison: Docker vs Kubernetes Networking

### Docker Compose Networking

```yaml
# docker-compose.yml
services:
  backend:
    networks:
      - ecommerce-network

networks:
  ecommerce-network:
    driver: bridge
```

**Characteristics:**
- **Bridge network** on single host
- **Container names** for DNS
- **Port mapping** to host
- **Simple** but limited to one host

### Docker Swarm Networking

```yaml
# docker-stack.yml
services:
  backend:
    networks:
      - ecommerce-overlay

networks:
  ecommerce-overlay:
    driver: overlay
```

**Characteristics:**
- **Overlay network** across nodes
- **Service discovery** via service names
- **Load balancing** via Swarm
- **Multi-host** support

### Kubernetes Networking

```yaml
# k8s/backend/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  type: ClusterIP
```

**Characteristics:**
- **IP-per-Pod** model
- **Service abstraction** for stable endpoints
- **DNS-based** service discovery
- **Network Policies** for security
- **Ingress** for HTTP routing
- **Most flexible** and production-ready

### Feature Comparison

| Feature | Docker Compose | Docker Swarm | Kubernetes |
|---------|---------------|--------------|------------|
| Network Type | Bridge | Overlay | CNI Plugin |
| Service Discovery | Container names | Service names | DNS + Services |
| Load Balancing | None | Built-in | Service + Ingress |
| Multi-host | ❌ | ✅ | ✅ |
| Network Policies | ❌ | ❌ | ✅ |
| Ingress | ❌ | ❌ | ✅ |
| DNS | Basic | Basic | Advanced (CoreDNS) |

---

## Best Practices

### 1. Use Services, Not Pod IPs

❌ **Bad:**
```java
String dbUrl = "jdbc:postgresql://10.244.1.7:5432/db";
```

✅ **Good:**
```java
String dbUrl = "jdbc:postgresql://postgres-service:5432/db";
```

### 2. Use Namespace-Scoped Services

❌ **Bad:**
```yaml
# Hardcoded namespace
url: jdbc:postgresql://postgres-service.ecommerce-prod:5432/db
```

✅ **Good:**
```yaml
# Same namespace (default)
url: jdbc:postgresql://postgres-service:5432/db
```

### 3. Implement Network Policies

✅ **Always:**
- Start with default deny-all
- Explicitly allow required traffic
- Test policies in dev first

### 4. Use Ingress for HTTP/HTTPS

✅ **For web traffic:**
- Use Ingress, not NodePort/LoadBalancer
- Enable TLS
- Use path-based routing

### 5. Monitor Service Endpoints

```bash
# Regularly check endpoints
kubectl get endpoints -n ecommerce

# Alert if endpoints = 0
```

---

## Summary

Kubernetes networking provides:

1. **Pod Networking**: Each pod gets unique IP, direct communication
2. **Services**: Stable endpoints, load balancing, service discovery
3. **Ingress**: HTTP/HTTPS routing, TLS termination
4. **Network Policies**: Fine-grained security controls
5. **DNS**: Automatic service discovery via CoreDNS

**In Our Platform:**
- Frontend → Backend via `backend-service`
- Backend → PostgreSQL via `postgres-service` (headless)
- Backend → Redis via `redis-service`
- External → Services via Ingress Controller
- All traffic secured by Network Policies

Understanding these concepts is essential for deploying, scaling, and securing containerized applications in Kubernetes.

---

For more information:
- [Kubernetes Networking Documentation](https://kubernetes.io/docs/concepts/services-networking/)
- [Service Documentation](https://kubernetes.io/docs/concepts/services-networking/service/)
- [Ingress Documentation](https://kubernetes.io/docs/concepts/services-networking/ingress/)
- [Network Policies Documentation](https://kubernetes.io/docs/concepts/services-networking/network-policies/)

