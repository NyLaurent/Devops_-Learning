# Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying the E-Commerce Platform to a Kubernetes cluster.

## 📁 Directory Structure

```
k8s/
├── base/                    # Base resources shared across environments
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml.template
│   └── network-policy.yaml
├── backend/                 # Backend service resources
│   ├── deployment.yaml
│   ├── service.yaml
│   └── hpa.yaml
├── frontend/                # Frontend service resources
│   ├── deployment.yaml
│   └── service.yaml
├── postgres/                # PostgreSQL database
│   ├── statefulset.yaml
│   └── service.yaml
├── redis/                   # Redis cache
│   ├── deployment.yaml
│   └── service.yaml
├── ingress/                 # Ingress configuration
│   └── ingress.yaml
└── environments/            # Environment-specific overlays
    ├── dev/
    │   └── kustomization.yaml
    └── prod/
        └── kustomization.yaml
```

## 🚀 Quick Start

### Prerequisites

1. **Kubernetes Cluster**: 
   - Local: [minikube](https://minikube.sigs.k8s.io/docs/start/), [kind](https://kind.sigs.k8s.io/), or [k3d](https://k3d.io/)
   - Cloud: EKS, GKE, AKS, or any K8s cluster

2. **kubectl**: Kubernetes command-line tool
3. **kustomize**: For environment overlays (included in kubectl 1.14+)

### 1. Prepare Secrets

```bash
# Copy the template and edit with your values
cp k8s/base/secrets.yaml.template k8s/base/secrets.yaml

# Edit secrets.yaml with actual values
# IMPORTANT: Never commit secrets.yaml to version control!
```

### 2. Update Image References

Edit the deployment files to use your container registry:

```bash
# Replace <registry> with your actual registry
# Example: docker.io/yourusername/devop-app-backend:latest
# Or: registry.gitlab.com/yourgroup/devop-app/backend:latest
```

### 3. Deploy to Development

```bash
# Apply base resources
kubectl apply -k k8s/environments/dev

# Verify deployment
kubectl get pods -n ecommerce-dev
kubectl get services -n ecommerce-dev
```

### 4. Deploy to Production

```bash
# Apply production resources
kubectl apply -k k8s/environments/prod

# Monitor rollout
kubectl rollout status deployment/backend -n ecommerce-prod
```

## 📋 Resource Overview

### Pods
- **Backend Pods**: Run Spring Boot application
- **Frontend Pods**: Run Nginx serving React build
- **PostgreSQL Pod**: Stateful database pod
- **Redis Pod**: Cache pod

### Deployments
- **Backend Deployment**: 2 replicas (dev) / 3 replicas (prod)
- **Frontend Deployment**: 2 replicas (dev) / 3 replicas (prod)
- **Redis Deployment**: 1 replica

### StatefulSet
- **PostgreSQL StatefulSet**: 1 replica with persistent storage

### Services
- **backend-service**: ClusterIP service for backend pods
- **frontend-service**: ClusterIP service for frontend pods
- **postgres-service**: Headless service for PostgreSQL
- **redis-service**: ClusterIP service for Redis

### Ingress
- Routes `/` → frontend-service
- Routes `/api/*` → backend-service
- Routes `/swagger-ui.html` → backend-service

### Network Policies
- Default deny-all policy
- Frontend → Backend communication allowed
- Backend → PostgreSQL/Redis communication allowed
- Isolated namespace traffic

## 🔧 Common Operations

### View Resources

```bash
# List all pods
kubectl get pods -n ecommerce-dev

# List services
kubectl get services -n ecommerce-dev

# Describe a pod
kubectl describe pod <pod-name> -n ecommerce-dev

# View logs
kubectl logs -f deployment/backend -n ecommerce-dev
```

### Scaling

```bash
# Scale backend manually
kubectl scale deployment backend --replicas=5 -n ecommerce-dev

# HPA will auto-scale in production (see backend/hpa.yaml)
kubectl get hpa -n ecommerce-prod
```

### Updates

```bash
# Update image
kubectl set image deployment/backend backend=<registry>/devop-app/backend:v1.1.0 -n ecommerce-dev

# Rollout status
kubectl rollout status deployment/backend -n ecommerce-dev

# Rollback
kubectl rollout undo deployment/backend -n ecommerce-dev
```

### Troubleshooting

```bash
# Check pod events
kubectl describe pod <pod-name> -n ecommerce-dev

# Execute command in pod
kubectl exec -it <pod-name> -n ecommerce-dev -- /bin/sh

# Port forward for local access
kubectl port-forward service/backend-service 8080:8080 -n ecommerce-dev
kubectl port-forward service/frontend-service 3000:3000 -n ecommerce-dev
```

## 🔐 Security Best Practices

1. **Secrets Management**: Use Kubernetes Secrets or external secret managers (Vault, AWS Secrets Manager)
2. **Network Policies**: Already configured to restrict pod-to-pod communication
3. **RBAC**: Set up Role-Based Access Control for namespace access
4. **Image Security**: Scan container images for vulnerabilities
5. **TLS**: Enable TLS in Ingress for production

## 📊 Monitoring & Observability

### Health Checks
- **Liveness Probes**: Restart unhealthy pods
- **Readiness Probes**: Remove pods from service endpoints when not ready

### Metrics
- Backend exposes `/actuator/metrics` endpoint
- Integrate with Prometheus for metrics collection
- Use Grafana for visualization

### Logging
- Use centralized logging (ELK, Loki, CloudWatch)
- Consider sidecar containers for log aggregation

## 🔄 CI/CD Integration

### Example GitLab CI

```yaml
deploy:
  stage: deploy
  script:
    - kubectl set image deployment/backend backend=$CI_REGISTRY_IMAGE/backend:$CI_COMMIT_TAG -n ecommerce-prod
    - kubectl rollout status deployment/backend -n ecommerce-prod
```

### Example GitHub Actions

```yaml
- name: Deploy to K8s
  run: |
    kubectl set image deployment/backend backend=${{ env.REGISTRY }}/backend:${{ github.sha }} -n ecommerce-prod
    kubectl rollout status deployment/backend -n ecommerce-prod
```

## 📚 Learning Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kustomize Documentation](https://kustomize.io/)

## 🆘 Troubleshooting Guide

| Issue | Solution |
|-------|----------|
| Pods in Pending state | Check node resources: `kubectl describe node` |
| Image pull errors | Verify image exists and credentials are correct |
| Service not accessible | Check service selector matches pod labels |
| Database connection fails | Verify postgres-service DNS resolution |
| Network policy blocking traffic | Review network-policy.yaml rules |

## 🎯 Next Steps

1. Set up Ingress Controller (nginx, traefik)
2. Configure TLS certificates (cert-manager)
3. Implement monitoring stack (Prometheus + Grafana)
4. Set up CI/CD pipeline
5. Configure backup strategy for PostgreSQL
6. Implement service mesh (Istio/Linkerd) for advanced traffic management

---

For detailed information about each resource, refer to the Kubernetes documentation or examine the YAML files directly.

