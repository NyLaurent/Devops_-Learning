# Kubernetes Security, Secrets & Environment Variables

This comprehensive guide explains how Kubernetes handles secrets, environment variables, and security best practices, using our E-Commerce Platform as a practical example.

## 📚 Table of Contents

1. [Secrets Management](#secrets-management)
2. [ConfigMaps](#configmaps)
3. [Environment Variables](#environment-variables)
4. [Security Best Practices](#security-best-practices)
5. [RBAC (Role-Based Access Control)](#rbac-role-based-access-control)
6. [Pod Security Standards](#pod-security-standards)
7. [Image Security](#image-security)
8. [Network Security](#network-security)
9. [Security Context](#security-context)
10. [Our Platform's Security Implementation](#our-platforms-security-implementation)

---

## Secrets Management

### What are Secrets?

**Secrets** are Kubernetes objects for storing **sensitive data** such as:
- Passwords
- API keys
- TLS certificates
- Database credentials
- OAuth tokens

### Secret Types

Kubernetes supports several built-in secret types:

| Type | Purpose | Example |
|------|---------|---------|
| `Opaque` | Generic secret (default) | Passwords, API keys |
| `kubernetes.io/tls` | TLS certificates | SSL/TLS certs |
| `kubernetes.io/dockerconfigjson` | Docker registry auth | Image pull secrets |
| `kubernetes.io/basic-auth` | Basic authentication | Username/password |

### Creating Secrets

#### Method 1: From Literal Values

```bash
kubectl create secret generic app-secrets \
  --from-literal=POSTGRES_PASSWORD=mySecurePassword123 \
  --from-literal=POSTGRES_USER=ecommerce \
  --from-literal=POSTGRES_DB=ecommerce \
  -n ecommerce
```

#### Method 2: From Files

```bash
# Create files
echo -n 'mySecurePassword123' > postgres-password.txt
echo -n 'ecommerce' > postgres-user.txt

# Create secret from files
kubectl create secret generic app-secrets \
  --from-file=POSTGRES_PASSWORD=postgres-password.txt \
  --from-file=POSTGRES_USER=postgres-user.txt \
  -n ecommerce
```

#### Method 3: From YAML (Base64 Encoded)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: ecommerce
type: Opaque
data:
  POSTGRES_PASSWORD: bXlTZWN1cmVQYXNzd29yZDEyMw==  # Base64 encoded
  POSTGRES_USER: ZWNvbW1lcmNl                      # Base64 encoded
  POSTGRES_DB: ZWNvbW1lcmNl                        # Base64 encoded
```

**Encoding/Decoding:**
```bash
# Encode
echo -n 'mySecurePassword123' | base64
# Output: bXlTZWN1cmVQYXNzd29yZDEyMw==

# Decode
echo 'bXlTZWN1cmVQYXNzd29yZDEyMw==' | base64 -d
# Output: mySecurePassword123
```

#### Method 4: Using stringData (Plain Text)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: ecommerce
type: Opaque
stringData:  # Kubernetes encodes this automatically
  POSTGRES_PASSWORD: mySecurePassword123
  POSTGRES_USER: ecommerce
  POSTGRES_DB: ecommerce
```

**Our Platform Template:**

```yaml
# k8s/base/secrets.yaml.template
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: ecommerce
type: Opaque
stringData:
  POSTGRES_DB: "ecommerce"
  POSTGRES_USER: "ecommerce"
  POSTGRES_PASSWORD: "CHANGE_ME_IN_PRODUCTION"
  SPRING_PROFILES_ACTIVE: "kubernetes"
```

### Using Secrets in Pods

#### Method 1: Environment Variables

```yaml
# backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  template:
    spec:
      containers:
      - name: backend
        env:
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: POSTGRES_PASSWORD
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: POSTGRES_USER
```

**In Our Platform:**

```yaml
# k8s/backend/deployment.yaml
env:
- name: POSTGRES_DB
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_DB
- name: POSTGRES_USER
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_USER
- name: POSTGRES_PASSWORD
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_PASSWORD
```

#### Method 2: Volume Mounts

```yaml
spec:
  containers:
  - name: backend
    volumeMounts:
    - name: secrets
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secrets
    secret:
      secretName: app-secrets
```

**File Structure in Pod:**
```
/etc/secrets/
├── POSTGRES_PASSWORD  (file content: mySecurePassword123)
├── POSTGRES_USER      (file content: ecommerce)
└── POSTGRES_DB        (file content: ecommerce)
```

**Access in Application:**
```java
// Read from file
String password = Files.readString(Path.of("/etc/secrets/POSTGRES_PASSWORD"));
```

#### Method 3: envFrom (All Keys)

```yaml
spec:
  containers:
  - name: backend
    envFrom:
    - secretRef:
        name: app-secrets
```

**Result:** All keys from `app-secrets` become environment variables.

### TLS Secrets

**Creating TLS Secret:**
```bash
kubectl create secret tls ecommerce-tls \
  --cert=tls.crt \
  --key=tls.key \
  -n ecommerce
```

**Using in Ingress:**
```yaml
# k8s/ingress/ingress.yaml
spec:
  tls:
  - hosts:
    - ecommerce.local
    secretName: ecommerce-tls
```

### Docker Registry Secrets

**Creating Registry Secret:**
```bash
kubectl create secret docker-registry regcred \
  --docker-server=registry.example.com \
  --docker-username=myuser \
  --docker-password=mypassword \
  --docker-email=myemail@example.com \
  -n ecommerce
```

**Using in Deployment:**
```yaml
spec:
  template:
    spec:
      imagePullSecrets:
      - name: regcred
      containers:
      - name: backend
        image: registry.example.com/devop-app/backend:latest
```

### Secret Best Practices

✅ **Do:**
- Use `stringData` in YAML for readability (K8s encodes automatically)
- Rotate secrets regularly
- Use external secret managers (Vault, AWS Secrets Manager) for production
- Restrict access via RBAC
- Never commit secrets to version control

❌ **Don't:**
- Store secrets in ConfigMaps
- Hardcode secrets in images
- Log secret values
- Share secrets via unencrypted channels

---

## ConfigMaps

### What are ConfigMaps?

**ConfigMaps** store **non-sensitive configuration data**:
- Configuration files
- Environment variables (non-secret)
- Command-line arguments
- Application settings

### Creating ConfigMaps

#### Method 1: From Literal Values

```bash
kubectl create configmap backend-config \
  --from-literal=server.port=8080 \
  --from-literal=spring.profiles.active=kubernetes \
  -n ecommerce
```

#### Method 2: From Files

```bash
kubectl create configmap nginx-config \
  --from-file=nginx.conf=./frontend/nginx.conf \
  -n ecommerce
```

#### Method 3: From YAML

```yaml
# k8s/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: backend-config
  namespace: ecommerce
data:
  application.yml: |
    server:
      port: 8080
    spring:
      application:
        name: product-service
      datasource:
        url: jdbc:postgresql://postgres-service:5432/${POSTGRES_DB}
        username: ${POSTGRES_USER}
        password: ${POSTGRES_PASSWORD}
```

**Our Platform ConfigMaps:**

1. **backend-config**: Spring Boot configuration
2. **nginx-config**: Nginx configuration for frontend

### Using ConfigMaps in Pods

#### Method 1: Environment Variables

```yaml
spec:
  containers:
  - name: backend
    env:
    - name: SERVER_PORT
      valueFrom:
        configMapKeyRef:
          name: backend-config
          key: server.port
```

#### Method 2: envFrom (All Keys)

```yaml
spec:
  containers:
  - name: backend
    envFrom:
    - configMapRef:
        name: backend-config
```

#### Method 3: Volume Mounts (Files)

```yaml
# k8s/backend/deployment.yaml
spec:
  containers:
  - name: backend
    volumeMounts:
    - name: config
      mountPath: /app/config
      readOnly: true
  volumes:
  - name: config
    configMap:
      name: backend-config
```

**File Structure:**
```
/app/config/
└── application.yml  (content from ConfigMap)
```

#### Method 4: Specific Key as File

```yaml
# k8s/frontend/deployment.yaml
spec:
  containers:
  - name: frontend
    volumeMounts:
    - name: nginx-config
      mountPath: /etc/nginx/nginx.conf
      subPath: nginx.conf  # Mount specific key as file
      readOnly: true
  volumes:
  - name: nginx-config
    configMap:
      name: nginx-config
```

### ConfigMap vs Secret

| Feature | ConfigMap | Secret |
|---------|-----------|--------|
| **Data Type** | Non-sensitive | Sensitive |
| **Encoding** | Plain text | Base64 (in YAML) |
| **Use Case** | Configuration files | Passwords, keys |
| **Size Limit** | 1MB | 1MB |
| **Storage** | etcd (plain text) | etcd (base64) |

**Important:** ConfigMaps are **NOT encrypted** in etcd. Use Secrets for sensitive data.

---

## Environment Variables

### Injection Methods

#### 1. Direct Value

```yaml
env:
- name: ENVIRONMENT
  value: "production"
```

#### 2. From ConfigMap

```yaml
env:
- name: SERVER_PORT
  valueFrom:
    configMapKeyRef:
      name: backend-config
      key: server.port
```

#### 3. From Secret

```yaml
env:
- name: POSTGRES_PASSWORD
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_PASSWORD
```

#### 4. From Field (Pod/Node Info)

```yaml
env:
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_IP
  valueFrom:
    fieldRef:
      fieldPath: status.podIP
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
```

**Available Fields:**
- `metadata.name` - Pod name
- `metadata.namespace` - Namespace
- `metadata.uid` - Pod UID
- `spec.nodeName` - Node name
- `status.podIP` - Pod IP
- `status.hostIP` - Node IP

#### 5. From Resource (Resource Quotas)

```yaml
env:
- name: CPU_LIMIT
  valueFrom:
    resourceFieldRef:
      resource: limits.cpu
- name: MEMORY_LIMIT
  valueFrom:
    resourceFieldRef:
      resource: limits.memory
```

### Our Platform's Environment Variables

**Backend Deployment:**
```yaml
# k8s/backend/deployment.yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "kubernetes"
- name: POSTGRES_HOST
  value: "postgres-service"
- name: POSTGRES_PORT
  value: "5432"
- name: POSTGRES_DB
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_DB
- name: POSTGRES_USER
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_USER
- name: POSTGRES_PASSWORD
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_PASSWORD
```

**Frontend Deployment:**
```yaml
# k8s/frontend/deployment.yaml
env:
- name: REACT_APP_API_URL
  value: "http://backend-service:8080/api"
```

**PostgreSQL StatefulSet:**
```yaml
# k8s/postgres/statefulset.yaml
env:
- name: POSTGRES_DB
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_DB
- name: POSTGRES_USER
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_USER
- name: POSTGRES_PASSWORD
  valueFrom:
    secretKeyRef:
      name: app-secrets
      key: POSTGRES_PASSWORD
- name: PGDATA
  value: "/var/lib/postgresql/data/pgdata"
```

---

## Security Best Practices

### 1. Secret Management

#### ✅ Use External Secret Managers

**Vault Integration:**
```yaml
# Install Vault Agent Injector
# Annotations automatically inject secrets
annotations:
  vault.hashicorp.com/agent-inject: "true"
  vault.hashicorp.com/role: "ecommerce"
  vault.hashicorp.com/agent-inject-secret-db: "secret/data/ecommerce/db"
```

**AWS Secrets Manager:**
```yaml
# Use External Secrets Operator
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: app-secrets
spec:
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: app-secrets
  data:
  - secretKey: POSTGRES_PASSWORD
    remoteRef:
      key: ecommerce/db
      property: password
```

#### ✅ Rotate Secrets Regularly

```bash
# Update secret
kubectl create secret generic app-secrets \
  --from-literal=POSTGRES_PASSWORD=newPassword \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart pods to pick up new secret
kubectl rollout restart deployment/backend -n ecommerce
```

#### ✅ Use Secret Encryption at Rest

**Enable Encryption:**
```yaml
# kube-apiserver configuration
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
- resources:
  - secrets
  providers:
  - aescbc:
      keys:
      - name: key1
        secret: <base64-encoded-key>
```

### 2. RBAC (Role-Based Access Control)

#### Roles and RoleBindings

**Create Role:**
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: ecommerce-developer
  namespace: ecommerce
rules:
- apiGroups: [""]
  resources: ["pods", "services"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "update"]
```

**Bind Role to User:**
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ecommerce-developer-binding
  namespace: ecommerce
subjects:
- kind: User
  name: developer@example.com
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role
  name: ecommerce-developer
  apiGroup: rbac.authorization.k8s.io
```

**ClusterRole and ClusterRoleBinding:**
```yaml
# Cluster-wide permissions
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-admin
rules:
- apiGroups: ["*"]
  resources: ["*"]
  verbs: ["*"]
```

#### Service Accounts

**Create Service Account:**
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: backend-sa
  namespace: ecommerce
```

**Use in Deployment:**
```yaml
# k8s/backend/deployment.yaml
spec:
  template:
    spec:
      serviceAccountName: backend-sa
      containers:
      - name: backend
```

**RBAC for Service Account:**
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: backend-role
  namespace: ecommerce
rules:
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: backend-role-binding
  namespace: ecommerce
subjects:
- kind: ServiceAccount
  name: backend-sa
  namespace: ecommerce
roleRef:
  kind: Role
  name: backend-role
  apiGroup: rbac.authorization.k8s.io
```

### 3. Pod Security Standards

Kubernetes provides three security levels:

#### Privileged (Least Secure)
```yaml
# Allows almost everything
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce-dev
  labels:
    pod-security.kubernetes.io/enforce: privileged
```

#### Baseline (Recommended for Dev)
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce-dev
  labels:
    pod-security.kubernetes.io/enforce: baseline
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

#### Restricted (Recommended for Prod)
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce-prod
  labels:
    pod-security.kubernetes.io/enforce: restricted
```

**Apply to Our Platform:**
```yaml
# k8s/base/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce
  labels:
    pod-security.kubernetes.io/enforce: baseline
    pod-security.kubernetes.io/audit: restricted
```

### 4. Security Context

**Pod-Level Security Context:**
```yaml
# k8s/backend/deployment.yaml
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      containers:
      - name: backend
```

**Container-Level Security Context:**
```yaml
containers:
- name: backend
  securityContext:
    allowPrivilegeEscalation: false
    readOnlyRootFilesystem: true
    capabilities:
      drop:
      - ALL
      add:
      - NET_BIND_SERVICE  # Only if needed
```

**Our Platform Implementation:**
```yaml
# k8s/backend/deployment.yaml
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001  # ecommerce user from Dockerfile
        fsGroup: 1001
      containers:
      - name: backend
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: false  # Need write for logs
          capabilities:
            drop:
            - ALL
```

### 5. Image Security

#### Use Specific Tags, Not `latest`

❌ **Bad:**
```yaml
image: postgres:latest
```

✅ **Good:**
```yaml
image: postgres:15-alpine
```

#### Scan Images for Vulnerabilities

```bash
# Using Trivy
trivy image postgres:15-alpine

# Using Docker Scout
docker scout cves postgres:15-alpine
```

#### Use Minimal Base Images

✅ **Good:**
```dockerfile
FROM eclipse-temurin:21-jre-slim
```

❌ **Bad:**
```dockerfile
FROM ubuntu:latest
RUN apt-get install openjdk-21
```

#### Sign and Verify Images

```bash
# Sign image
cosign sign --key cosign.key <registry>/devop-app/backend:latest

# Verify in deployment
# Use admission controller to enforce
```

---

## Network Security

### Network Policies

Already covered in `K8S_NETWORKING.md`, but key points:

**Our Platform's Network Policies:**
- Default deny-all
- Frontend → Backend (port 8080)
- Backend → PostgreSQL (port 5432)
- Backend → Redis (port 6379)
- DNS access (port 53) for all

### TLS/HTTPS

**Enable TLS in Ingress:**
```yaml
# k8s/ingress/ingress.yaml
spec:
  tls:
  - hosts:
    - ecommerce.local
    secretName: ecommerce-tls
```

**Use cert-manager for Automatic TLS:**
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: ecommerce-tls
  namespace: ecommerce
spec:
  secretName: ecommerce-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - ecommerce.local
```

---

## Security Context

### Run as Non-Root

**Dockerfile:**
```dockerfile
# backend/Dockerfile
RUN groupadd -r ecommerce && useradd -r -g ecommerce ecommerce
USER ecommerce
```

**Kubernetes:**
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1001
  fsGroup: 1001
```

### Read-Only Root Filesystem

```yaml
securityContext:
  readOnlyRootFilesystem: true
volumeMounts:
- name: tmp
  mountPath: /tmp
- name: logs
  mountPath: /app/logs
volumes:
- name: tmp
  emptyDir: {}
- name: logs
  emptyDir: {}
```

### Drop Capabilities

```yaml
securityContext:
  capabilities:
    drop:
    - ALL
    add: []  # Only add what's absolutely necessary
```

---

## Our Platform's Security Implementation

### Complete Security Stack

```
┌─────────────────────────────────────────────────┐
│  Security Layers                                │
└─────────────────────────────────────────────────┘

1. Namespace Isolation
   └─ ecommerce namespace with Pod Security Standards

2. RBAC
   └─ Service accounts with minimal permissions

3. Secrets Management
   └─ app-secrets for sensitive data
   └─ External secret manager (production)

4. Network Policies
   └─ Default deny-all
   └─ Explicit allow rules

5. Security Context
   └─ Run as non-root
   └─ Drop all capabilities
   └─ Read-only root filesystem (where possible)

6. Image Security
   └─ Specific tags (not latest)
   └─ Minimal base images
   └─ Regular vulnerability scanning

7. TLS/HTTPS
   └─ Ingress TLS termination
   └─ cert-manager for automatic certs
```

### Security Checklist

✅ **Implemented:**
- [x] Secrets for sensitive data (passwords, keys)
- [x] ConfigMaps for non-sensitive config
- [x] Network Policies for traffic control
- [x] Security Context (non-root, capabilities)
- [x] Namespace isolation
- [x] Specific image tags

⚠️ **To Implement:**
- [ ] RBAC for service accounts
- [ ] External secret manager (Vault/AWS Secrets Manager)
- [ ] Image signing and verification
- [ ] Pod Security Standards enforcement
- [ ] TLS/HTTPS with cert-manager
- [ ] Regular secret rotation
- [ ] Vulnerability scanning in CI/CD

---

## Troubleshooting Security Issues

### Secret Not Accessible

**Symptoms:**
- Pod can't read secret
- Environment variable empty

**Debug:**
```bash
# Check secret exists
kubectl get secret app-secrets -n ecommerce

# Check secret keys
kubectl get secret app-secrets -o jsonpath='{.data}' -n ecommerce

# Check pod can access
kubectl exec -it backend-xxx -- env | grep POSTGRES

# Check RBAC permissions
kubectl auth can-i get secrets --as=system:serviceaccount:ecommerce:backend-sa -n ecommerce
```

### Permission Denied Errors

**Symptoms:**
- Pod can't write to filesystem
- Permission denied errors

**Debug:**
```bash
# Check security context
kubectl get pod backend-xxx -o yaml | grep -A 10 securityContext

# Check user running
kubectl exec -it backend-xxx -- whoami

# Check file permissions
kubectl exec -it backend-xxx -- ls -la /app
```

### Network Policy Blocking

**Symptoms:**
- Pods can't communicate
- Connection refused

**Debug:**
```bash
# List network policies
kubectl get networkpolicies -n ecommerce

# Describe policy
kubectl describe networkpolicy backend-policy -n ecommerce

# Temporarily remove to test
kubectl delete networkpolicy backend-policy -n ecommerce
```

---

## Best Practices Summary

### Secrets
1. ✅ Never commit secrets to version control
2. ✅ Use external secret managers for production
3. ✅ Rotate secrets regularly
4. ✅ Use `stringData` for readability
5. ✅ Restrict access via RBAC

### ConfigMaps
1. ✅ Use for non-sensitive configuration
2. ✅ Keep under 1MB limit
3. ✅ Use volume mounts for files
4. ✅ Update ConfigMaps, restart pods

### Environment Variables
1. ✅ Use secrets for sensitive data
2. ✅ Use ConfigMaps for non-sensitive data
3. ✅ Use fieldRef for pod/node info
4. ✅ Document all environment variables

### Security
1. ✅ Run as non-root user
2. ✅ Drop all capabilities
3. ✅ Use read-only root filesystem
4. ✅ Implement Network Policies
5. ✅ Enable RBAC
6. ✅ Scan images for vulnerabilities
7. ✅ Use specific image tags
8. ✅ Enable TLS/HTTPS

---

## Summary

Kubernetes provides multiple layers of security:

1. **Secrets**: Encrypted storage for sensitive data
2. **ConfigMaps**: Non-sensitive configuration
3. **RBAC**: Fine-grained access control
4. **Network Policies**: Network-level security
5. **Security Context**: Container-level security
6. **Pod Security Standards**: Namespace-level policies

**In Our Platform:**
- Secrets store database credentials
- ConfigMaps store application configuration
- Network Policies restrict pod communication
- Security Context runs containers as non-root
- Namespace isolation separates environments

Understanding these concepts is essential for securing containerized applications in Kubernetes.

---

For more information:
- [Kubernetes Secrets Documentation](https://kubernetes.io/docs/concepts/configuration/secret/)
- [ConfigMaps Documentation](https://kubernetes.io/docs/concepts/configuration/configmap/)
- [RBAC Documentation](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/)

