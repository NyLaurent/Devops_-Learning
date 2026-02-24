# Kubernetes Guide for E-Commerce Platform

This guide explains how Kubernetes fits into our container orchestration journey and how to use it with the E-Commerce Platform.

## 🎯 Why Kubernetes?

Our platform now supports three orchestration approaches:

1. **Docker Compose** → Local development, single-host
2. **Docker Swarm** → Simple multi-node orchestration
3. **Kubernetes** → Production-grade, enterprise-ready orchestration

Kubernetes provides:
- **Automatic scaling** based on CPU/memory metrics
- **Self-healing** with automatic pod restarts
- **Rolling updates** with zero downtime
- **Service discovery** via DNS
- **Network policies** for security
- **Resource management** with limits and requests

## 📊 Architecture Comparison

### Docker Compose
```
┌─────────────────────────────────────┐
│  Single Host                        │
│  ┌──────────┐  ┌──────────┐        │
│  │ Backend  │  │ Frontend │        │
│  └──────────┘  └──────────┘        │
│  ┌──────────┐                      │
│  │ Postgres │                      │
│  └──────────┘                      │
└─────────────────────────────────────┘
```

### Docker Swarm
```
┌──────────────┐    ┌──────────────┐
│ Manager Node │    │ Worker Node  │
│ ┌──────────┐ │    │ ┌──────────┐ │
│ │ Backend  │ │    │ │ Frontend │ │
│ └──────────┘ │    │ └──────────┘ │
└──────────────┘    └──────────────┘
```

### Kubernetes
```
┌─────────────────────────────────────────┐
│  Kubernetes Cluster                     │
│  ┌──────────┐  ┌──────────┐            │
│  │  Pods    │  │ Services │            │
│  │ ┌──────┐ │  │ ┌──────┐ │            │
│  │ │Backend│ │  │ │Cluster││            │
│  │ └──────┘ │  │ │  IP   ││            │
│  └──────────┘  └──────────┘            │
│  ┌──────────┐  ┌──────────┐            │
│  │Ingress  │  │ Network  │            │
│  │Controller│  │ Policies │            │
│  └──────────┘  └──────────┘            │
└─────────────────────────────────────────┘
```

## 🔑 Key Kubernetes Concepts

### Pods
- **Smallest deployable unit** in Kubernetes
- Contains one or more containers
- Share network and storage
- Ephemeral (can be recreated)

**In our platform:**
- Backend pod: 1 container (Spring Boot app)
- Frontend pod: 1 container (Nginx)
- PostgreSQL pod: 1 container (Postgres)

### Deployments
- **Manages Pod replicas**
- Handles rolling updates
- Provides rollback capability
- Declares desired state

**In our platform:**
- `backend/deployment.yaml`: Manages backend pods
- `frontend/deployment.yaml`: Manages frontend pods
- Replicas: 2 (dev) or 3 (prod)

### Services
- **Stable network endpoint** for pods
- Load balances across pod replicas
- Provides service discovery via DNS

**Types we use:**
- **ClusterIP**: Internal service (backend, frontend, redis)
- **Headless**: Direct pod access (postgres)

### StatefulSets
- **For stateful applications** (databases)
- Maintains stable network identity
- Ordered deployment/scaling
- Persistent storage per pod

**In our platform:**
- `postgres/statefulset.yaml`: PostgreSQL with persistent volume

### Namespaces
- **Logical isolation** of resources
- Separate dev/staging/prod
- Resource quotas per namespace
- RBAC boundaries

**Our namespaces:**
- `ecommerce-dev`: Development
- `ecommerce-prod`: Production

### Network Policies
- **Firewall rules** for pods
- Controls ingress/egress traffic
- Default deny-all, explicit allow

**Our policies:**
- Frontend → Backend (port 8080)
- Backend → PostgreSQL (port 5432)
- Backend → Redis (port 6379)

### Ingress
- **External access** to services
- Path-based routing
- TLS termination
- Single entry point

**Our ingress:**
- `/` → frontend-service
- `/api/*` → backend-service
- `/swagger-ui.html` → backend-service

## 🚀 Getting Started

### Option 1: Local Development (minikube)

```bash
# Install minikube
# macOS: brew install minikube
# Linux: See https://minikube.sigs.k8s.io/docs/start/

# Start cluster
minikube start

# Enable ingress addon
minikube addons enable ingress

# Deploy application
kubectl apply -k k8s/environments/dev

# Access via minikube
minikube service frontend-service -n ecommerce-dev
```

### Option 2: Local Development (kind)

```bash
# Install kind
# macOS: brew install kind

# Create cluster
kind create cluster --name ecommerce

# Load images
kind load docker-image devop-app-backend:latest --name ecommerce
kind load docker-image devop-app-frontend:latest --name ecommerce

# Deploy
kubectl apply -k k8s/environments/dev
```

### Option 3: Cloud Provider

```bash
# AWS EKS
eksctl create cluster --name ecommerce-cluster

# Google GKE
gcloud container clusters create ecommerce-cluster

# Azure AKS
az aks create --resource-group ecommerce --name ecommerce-cluster
```

## 📝 Common Commands

### Deployment

```bash
# Apply all resources
kubectl apply -k k8s/environments/dev

# Delete all resources
kubectl delete -k k8s/environments/dev

# Check status
kubectl get all -n ecommerce-dev
```

### Scaling

```bash
# Manual scaling
kubectl scale deployment backend --replicas=5 -n ecommerce-dev

# Auto-scaling (HPA)
kubectl get hpa -n ecommerce-prod
kubectl describe hpa backend-hpa -n ecommerce-prod
```

### Updates

```bash
# Update image
kubectl set image deployment/backend \
  backend=registry.example.com/backend:v1.1.0 \
  -n ecommerce-dev

# Rollout status
kubectl rollout status deployment/backend -n ecommerce-dev

# Rollback
kubectl rollout undo deployment/backend -n ecommerce-dev

# View rollout history
kubectl rollout history deployment/backend -n ecommerce-dev
```

### Debugging

```bash
# View logs
kubectl logs -f deployment/backend -n ecommerce-dev

# Execute command in pod
kubectl exec -it <pod-name> -n ecommerce-dev -- /bin/sh

# Port forward
kubectl port-forward service/backend-service 8080:8080 -n ecommerce-dev

# Describe resources
kubectl describe pod <pod-name> -n ecommerce-dev
kubectl describe service backend-service -n ecommerce-dev
```

## 🔄 Migration Path

### From Docker Compose

1. **Extract configurations** → ConfigMaps
2. **Extract secrets** → Secrets
3. **Convert services** → Deployments/StatefulSets
4. **Network setup** → Services + Ingress
5. **Volumes** → PersistentVolumeClaims

### From Docker Swarm

1. **Services** → Deployments/StatefulSets
2. **Overlay networks** → Services (ClusterIP)
3. **Docker secrets** → Kubernetes Secrets
4. **Stack deploy** → `kubectl apply -k`

## 🎓 Learning Path

1. **Start with Pods**: Understand basic container execution
2. **Learn Deployments**: Manage pod replicas and updates
3. **Explore Services**: Service discovery and load balancing
4. **Study StatefulSets**: Stateful applications
5. **Master Ingress**: External access patterns
6. **Network Policies**: Security boundaries
7. **Advanced**: HPA, Operators, Service Mesh

## 📚 Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kubernetes Tutorial](https://kubernetes.io/docs/tutorials/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kustomize Guide](https://kustomize.io/)

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| Pods not starting | Check `kubectl describe pod` for events |
| Image pull errors | Verify image exists and credentials |
| Service not accessible | Check service selector matches pod labels |
| Database connection fails | Verify DNS: `kubectl get svc postgres-service` |
| Network policy blocking | Review `network-policy.yaml` rules |
| Out of resources | Check node capacity: `kubectl top nodes` |

---

Kubernetes provides the most robust and scalable orchestration solution for our platform, perfect for production deployments and learning enterprise-grade container management.

