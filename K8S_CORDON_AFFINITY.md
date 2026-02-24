# Kubernetes Cordon & Affinity: Complete Guide

This comprehensive guide explains **Cordon** and **Affinity** concepts in Kubernetes, when to use them, and how they apply to our E-Commerce Platform.

## 📚 Table of Contents

1. [Node Cordon](#node-cordon)
2. [Node Affinity](#node-affinity)
3. [Pod Affinity](#pod-affinity)
4. [Pod Anti-Affinity](#pod-anti-affinity)
5. [When to Use Each](#when-to-use-each)
6. [Our Platform Examples](#our-platform-examples)
7. [Comparison Table](#comparison-table)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

---

## Node Cordon

### What is Cordon?

**Cordon** marks a node as **unschedulable**, preventing new pods from being scheduled on that node. Existing pods continue running.

### Purpose

- **Maintenance**: Prepare node for maintenance without disrupting running pods
- **Drain Node**: First step before draining a node
- **Resource Management**: Prevent new workloads on specific nodes
- **Testing**: Isolate nodes for testing

### How It Works

```bash
# Cordon a node
kubectl cordon <node-name>

# Check node status
kubectl get nodes
# Output shows: STATUS = Ready,SchedulingDisabled
```

**What Happens:**
- ✅ Existing pods continue running
- ❌ New pods cannot be scheduled
- ✅ Pods can still be manually created (but won't start)
- ✅ Node remains in cluster

### Uncordon (Reverse)

```bash
# Make node schedulable again
kubectl uncordon <node-name>
```

### Example Scenario

**Maintenance Workflow:**
```bash
# 1. Cordon the node
kubectl cordon node-worker-1

# 2. Drain existing pods (moves them to other nodes)
kubectl drain node-worker-1 --ignore-daemonsets --delete-emptydir-data

# 3. Perform maintenance

# 4. Uncordon when done
kubectl uncordon node-worker-1
```

### Visual Representation

```
Before Cordon:
┌─────────────────────────────────────┐
│  Node: worker-1                     │
│  Status: Ready                      │
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │ Pod1 │  │ Pod2 │  │ Pod3 │     │
│  └──────┘  └──────┘  └──────┘     │
│       ↑                              │
│       │ New pods can be scheduled    │
└─────────────────────────────────────┘

After Cordon:
┌─────────────────────────────────────┐
│  Node: worker-1                     │
│  Status: Ready,SchedulingDisabled    │
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │ Pod1 │  │ Pod2 │  │ Pod3 │     │
│  └──────┘  └──────┘  └──────┘     │
│       ✗                              │
│       │ New pods CANNOT be scheduled │
└─────────────────────────────────────┘
```

---

## Node Affinity

### What is Node Affinity?

**Node Affinity** allows you to **constrain** which nodes your pods can be scheduled on, based on node labels.

### Types of Node Affinity

#### 1. Required (Hard Constraint)

**Must match** - Pod will not be scheduled if conditions aren't met.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-type
                operator: In
                values:
                - compute-optimized
```

#### 2. Preferred (Soft Constraint)

**Should match** - Scheduler tries to match, but pod can be scheduled elsewhere if not possible.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
              - key: node-type
                operator: In
                values:
                - compute-optimized
```

### Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `In` | Value in list | `values: ["compute", "gpu"]` |
| `NotIn` | Value not in list | `values: ["development"]` |
| `Exists` | Label exists | No `values` needed |
| `DoesNotExist` | Label doesn't exist | No `values` needed |
| `Gt` | Greater than (numeric) | `values: ["4"]` |
| `Lt` | Less than (numeric) | `values: ["8"]` |

### Example: Zone-Based Affinity

```yaml
# Deploy backend to specific availability zone
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: topology.kubernetes.io/zone
                operator: In
                values:
                - us-east-1a
                - us-east-1b
```

### Example: Instance Type Affinity

```yaml
# Prefer GPU nodes for ML workloads
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ml-service
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
              - key: instance-type
                operator: In
                values:
                - gpu-nvidia-t4
```

---

## Pod Affinity

### What is Pod Affinity?

**Pod Affinity** allows you to schedule pods **near** (on the same node or zone) as other pods with specific labels.

### Use Cases

- **Co-location**: Keep related pods together for performance
- **Data Locality**: Keep app pods near data pods
- **Network Proximity**: Reduce network latency

### Types

#### 1. Required (Hard)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  template:
    spec:
      affinity:
        podAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - backend
            topologyKey: kubernetes.io/hostname
```

**Meaning**: Frontend **must** be on the same node as backend.

#### 2. Preferred (Soft)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  template:
    spec:
      affinity:
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - backend
              topologyKey: kubernetes.io/hostname
```

**Meaning**: Frontend **prefers** to be on the same node as backend.

### Topology Keys

| Topology Key | Scope |
|-------------|-------|
| `kubernetes.io/hostname` | Same node |
| `topology.kubernetes.io/zone` | Same availability zone |
| `topology.kubernetes.io/region` | Same region |
| Custom labels | Custom topology |

### Example: Co-locate Frontend and Backend

```yaml
# Frontend prefers to be on same node as backend
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  template:
    metadata:
      labels:
        app: frontend
    spec:
      affinity:
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - backend
              topologyKey: kubernetes.io/hostname
```

---

## Pod Anti-Affinity

### What is Pod Anti-Affinity?

**Pod Anti-Affinity** prevents pods from being scheduled **near** (on the same node or zone) as other pods with specific labels.

### Use Cases

- **High Availability**: Spread replicas across nodes/zones
- **Resource Distribution**: Prevent resource contention
- **Fault Isolation**: Ensure pods don't share failure domains

### Types

#### 1. Required (Hard)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  replicas: 3
  template:
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - backend
            topologyKey: kubernetes.io/hostname
```

**Meaning**: Backend pods **cannot** be on the same node as other backend pods.

#### 2. Preferred (Soft)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  replicas: 3
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - backend
              topologyKey: kubernetes.io/hostname
```

**Meaning**: Backend pods **prefer** to be on different nodes.

### Example: Spread Across Zones

```yaml
# Ensure backend pods are in different availability zones
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  replicas: 3
  template:
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - backend
            topologyKey: topology.kubernetes.io/zone
```

---

## When to Use Each

### Use Cordon When:

✅ **Node Maintenance**
- Preparing node for updates
- Hardware replacement
- OS upgrades

✅ **Resource Management**
- Prevent new workloads on specific nodes
- Reserve nodes for specific purposes

✅ **Testing**
- Isolate nodes for testing
- Prevent test workloads from affecting production

✅ **Draining Nodes**
- First step before `kubectl drain`
- Graceful node removal

### Use Node Affinity When:

✅ **Hardware Requirements**
- GPU workloads need GPU nodes
- High-memory apps need memory-optimized nodes
- CPU-intensive apps need compute-optimized nodes

✅ **Compliance**
- Deploy to specific regions/zones
- Meet data residency requirements

✅ **Cost Optimization**
- Use cheaper instance types where possible
- Prefer spot instances for non-critical workloads

✅ **Performance**
- Place workloads on nodes with specific hardware
- Optimize for network or storage performance

### Use Pod Affinity When:

✅ **Performance Optimization**
- Co-locate related services (reduce latency)
- Keep app near its cache (Redis)
- Data locality (app near database)

✅ **Resource Sharing**
- Share local storage
- Share network interfaces
- Share memory between related pods

### Use Pod Anti-Affinity When:

✅ **High Availability**
- Spread replicas across nodes
- Prevent single point of failure
- Ensure fault tolerance

✅ **Resource Distribution**
- Prevent resource contention
- Balance load across nodes
- Avoid overloading single node

✅ **Multi-Zone Deployment**
- Spread across availability zones
- Regional redundancy
- Disaster recovery

---

## Our Platform Examples

### Example 1: High Availability Backend

**Goal**: Ensure backend replicas are on different nodes for HA.

```yaml
# k8s/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: ecommerce
spec:
  replicas: 3
  template:
    metadata:
      labels:
        app: backend
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - backend
              topologyKey: kubernetes.io/hostname
      containers:
      - name: backend
        image: <registry>/devop-app/backend:latest
```

**Result**: Backend pods prefer to be on different nodes.

### Example 2: Frontend Near Backend

**Goal**: Co-locate frontend with backend for reduced latency.

```yaml
# k8s/frontend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: ecommerce
spec:
  replicas: 2
  template:
    metadata:
      labels:
        app: frontend
    spec:
      affinity:
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 80
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - backend
              topologyKey: kubernetes.io/hostname
      containers:
      - name: frontend
        image: <registry>/devop-app/frontend:latest
```

**Result**: Frontend pods prefer to be on the same node as backend pods.

### Example 3: PostgreSQL on Database Nodes

**Goal**: Deploy PostgreSQL only on nodes labeled for databases.

```yaml
# k8s/postgres/statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: ecommerce
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-role
                operator: In
                values:
                - database
      containers:
      - name: postgres
        image: postgres:15-alpine
```

**Result**: PostgreSQL only runs on nodes with `node-role=database` label.

### Example 4: Multi-Zone Deployment

**Goal**: Spread backend across availability zones for disaster recovery.

```yaml
# k8s/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: ecommerce
spec:
  replicas: 3
  template:
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - backend
            topologyKey: topology.kubernetes.io/zone
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
              - key: topology.kubernetes.io/zone
                operator: In
                values:
                - us-east-1a
                - us-east-1b
                - us-east-1c
```

**Result**: Backend pods are in different zones, with preference for specific zones.

### Example 5: Redis Co-location

**Goal**: Keep Redis cache on the same node as backend for low latency.

```yaml
# k8s/redis/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: ecommerce
spec:
  replicas: 1
  template:
    spec:
      affinity:
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - backend
              topologyKey: kubernetes.io/hostname
      containers:
      - name: redis
        image: redis:7-alpine
```

**Result**: Redis prefers to be on the same node as backend.

---

## Comparison Table

| Feature | Cordon | Node Affinity | Pod Affinity | Pod Anti-Affinity |
|---------|--------|---------------|--------------|-------------------|
| **Purpose** | Prevent scheduling | Node selection | Co-location | Separation |
| **Scope** | Node-level | Node-level | Pod-to-pod | Pod-to-pod |
| **Type** | Command/Operation | Scheduling rule | Scheduling rule | Scheduling rule |
| **Persistence** | Until uncordon | Always active | Always active | Always active |
| **Use Case** | Maintenance | Hardware requirements | Performance | High availability |
| **Flexibility** | Binary (on/off) | Flexible (operators) | Flexible (topology) | Flexible (topology) |

### Hard vs Soft Constraints

| Type | Constraint | Behavior |
|------|-----------|----------|
| **Required** | Hard | Pod won't schedule if condition not met |
| **Preferred** | Soft | Scheduler tries to match, but can schedule elsewhere |

---

## Troubleshooting

### Cordon Issues

**Problem**: Node stuck in SchedulingDisabled

**Solution:**
```bash
# Check node status
kubectl get nodes

# Uncordon if needed
kubectl uncordon <node-name>

# Verify
kubectl describe node <node-name> | grep -i schedulable
```

**Problem**: Pods not scheduling after uncordon

**Solution:**
```bash
# Check node conditions
kubectl describe node <node-name>

# Check for taints
kubectl describe node <node-name> | grep -i taint

# Remove taints if needed
kubectl taint nodes <node-name> <taint-key>-
```

### Affinity Issues

**Problem**: Pods in Pending state

**Debug:**
```bash
# Check pod events
kubectl describe pod <pod-name> -n ecommerce

# Look for messages like:
# "0/3 nodes are available: 3 node(s) didn't match node affinity"

# Check node labels
kubectl get nodes --show-labels

# Verify affinity rules
kubectl get deployment backend -o yaml | grep -A 20 affinity
```

**Problem**: Pods not spreading as expected

**Debug:**
```bash
# Check pod distribution
kubectl get pods -o wide -n ecommerce

# Verify anti-affinity rules
kubectl get deployment backend -o yaml | grep -A 20 podAntiAffinity

# Check topology keys
kubectl get nodes -o custom-columns=NAME:.metadata.name,ZONE:.metadata.labels.topology\.kubernetes\.io/zone
```

**Problem**: Required affinity too strict

**Solution:**
```bash
# Change from required to preferred
# Edit deployment and change:
# requiredDuringSchedulingIgnoredDuringExecution
# to:
# preferredDuringSchedulingIgnoredDuringExecution
```

### Common Commands

```bash
# List nodes with labels
kubectl get nodes --show-labels

# Add label to node
kubectl label nodes <node-name> node-type=compute-optimized

# Remove label from node
kubectl label nodes <node-name> node-type-

# Check pod distribution
kubectl get pods -o wide -n ecommerce

# Describe pod to see scheduling decisions
kubectl describe pod <pod-name> -n ecommerce

# Check events
kubectl get events -n ecommerce --sort-by='.lastTimestamp'
```

---

## Best Practices

### Cordon Best Practices

1. ✅ **Always drain before maintenance**
   ```bash
   kubectl cordon <node>
   kubectl drain <node> --ignore-daemonsets --delete-emptydir-data
   ```

2. ✅ **Uncordon after maintenance**
   - Don't leave nodes cordoned
   - Verify node is healthy before uncordon

3. ✅ **Use with caution in production**
   - Cordon reduces cluster capacity
   - Ensure enough nodes remain

### Node Affinity Best Practices

1. ✅ **Use preferred for flexibility**
   - Hard constraints can prevent scheduling
   - Preferred allows fallback

2. ✅ **Label nodes consistently**
   ```bash
   # Standard labels
   node-type=compute-optimized
   node-role=database
   instance-type=gpu-nvidia-t4
   ```

3. ✅ **Combine with taints/tolerations**
   - Use taints to repel pods
   - Use affinity to attract pods

### Pod Affinity Best Practices

1. ✅ **Use for performance-critical co-location**
   - App + Cache (Redis)
   - App + Database (if local)

2. ✅ **Be careful with required affinity**
   - Can prevent scheduling
   - Use preferred when possible

3. ✅ **Consider network topology**
   - Same node = lowest latency
   - Same zone = low latency
   - Same region = medium latency

### Pod Anti-Affinity Best Practices

1. ✅ **Use for high availability**
   - Spread replicas across nodes
   - Spread across zones for DR

2. ✅ **Required for critical services**
   ```yaml
   # Database replicas must be on different nodes
   requiredDuringSchedulingIgnoredDuringExecution
   ```

3. ✅ **Preferred for non-critical**
   ```yaml
   # Frontend can share nodes if needed
   preferredDuringSchedulingIgnoredDuringExecution
   ```

4. ✅ **Use appropriate topology keys**
   - `kubernetes.io/hostname` for node-level
   - `topology.kubernetes.io/zone` for zone-level
   - Custom labels for custom topologies

---

## Complete Example: Production-Ready Deployment

```yaml
# k8s/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: ecommerce
spec:
  replicas: 3
  template:
    metadata:
      labels:
        app: backend
        tier: api
    spec:
      # Node Affinity: Prefer compute-optimized nodes
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
              - key: node-type
                operator: In
                values:
                - compute-optimized
        # Pod Anti-Affinity: Spread across nodes
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - backend
            topologyKey: kubernetes.io/hostname
        # Pod Affinity: Prefer same zone as Redis
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 50
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - redis
              topologyKey: topology.kubernetes.io/zone
      containers:
      - name: backend
        image: <registry>/devop-app/backend:latest
```

**What this achieves:**
- ✅ Prefers compute-optimized nodes
- ✅ Backend pods on different nodes (HA)
- ✅ Prefers same zone as Redis (performance)
- ✅ Flexible (uses preferred, not required)

---

## Summary

### Key Takeaways

1. **Cordon**: Temporarily prevent scheduling on a node (maintenance)
2. **Node Affinity**: Schedule pods on specific nodes (hardware requirements)
3. **Pod Affinity**: Co-locate pods (performance)
4. **Pod Anti-Affinity**: Separate pods (high availability)

### Decision Tree

```
Need to prevent scheduling on node?
├─ Yes → Use CORDON (temporary)
└─ No
   │
   Need pods on specific nodes?
   ├─ Yes → Use NODE AFFINITY
   └─ No
      │
      Need pods together?
      ├─ Yes → Use POD AFFINITY
      └─ No
         │
         Need pods separated?
         └─ Yes → Use POD ANTI-AFFINITY
```

### In Our Platform

- **Backend**: Pod anti-affinity for HA (spread across nodes)
- **Frontend**: Pod affinity with backend (co-location)
- **PostgreSQL**: Node affinity for database nodes
- **Redis**: Pod affinity with backend (performance)

Understanding these concepts is essential for:
- High availability deployments
- Performance optimization
- Resource management
- Maintenance operations

---

For more information:
- [Kubernetes Node Affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#node-affinity)
- [Pod Affinity and Anti-Affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity)
- [Cordon and Drain](https://kubernetes.io/docs/tasks/administer-cluster/safely-drain-node/)



