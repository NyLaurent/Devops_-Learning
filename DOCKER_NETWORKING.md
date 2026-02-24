# Docker Networking Explained

This guide summarizes the networking modes available in Docker and how they apply to our E-Commerce Platform. Understanding the differences ensures the right topology is used for local development, testing, and production Swarm deployments.

## 1. Key Concepts

- **Network**: A logical construct that Docker uses to connect containers. Each network provides isolated communication channels and DNS-based discovery.
- **Driver**: The implementation that provides the actual networking capabilities (bridge, host, overlay, etc.).
- **Endpoint**: A container’s attachment to a network. A single container can connect to multiple networks.
- **Service Discovery**: Docker DNS assigns container names/aliases that resolve to internal IPs for cross-container communication.

## 2. Default Networking Modes

### 2.1 `bridge` (default for standalone containers)
- **What**: Creates an isolated virtual network on the Docker host.
- **Use case**: Local development or single-host setups where containers need to communicate with each other.
- **How we use it**: In `docker-compose.yml`, the `ecommerce-network` uses the `bridge` driver. Backend, frontend, database, and optional services communicate securely while exposing necessary ports to the host.
- **Pros**:
  - DNS-based service discovery
  - Network isolation on a single host
  - Easy port publishing via `ports:`
- **Cons**:
  - Limited to a single host; cannot span multiple machines
- **Example**:
```bash
# Create a user-defined bridge network
docker network create --driver bridge demo-net

# Run two containers on that network
docker run -d --name demo-db --network demo-net postgres:15-alpine
docker run -d --name demo-api --network demo-net \
  -e POSTGRES_HOST=demo-db \
  my-api:latest

# Verify connectivity
docker exec demo-api ping -c2 demo-db
```

### 2.2 `host`
- **What**: Containers share the host’s network stack. No virtual network, no isolation.
- **Use case**: Performance-sensitive applications needing direct access to the host network. Often used for load balancers or monitoring agents.
- **Relevance**: Rarely needed in this project. Could be used for high-performance benchmarking of backend API or when using 3rd-party services that require binding to host interfaces.
- **Pros**:
  - No NAT/port mapping overhead
  - Direct access to host ports
- **Cons**:
  - No network isolation
  - Port conflicts; container must manage unique ports
- **Example**:
```bash
# Run Nginx using the host network (shares host ports)
docker run --rm --name edge --network host nginx:alpine

# Nginx now listens directly on the host's port 80 without publishing rules
curl http://localhost
```

### 2.3 `none`
- **What**: Containers receive a loopback interface only. No external networking.
- **Use case**: Strictly isolated workloads where you plan to handle networking manually (e.g., custom network namespaces with `ip netns`).
- **Relevance**: Rarely useful for application services; more for advanced security scenarios or debugging.
- **Example**:
```bash
# Start a container with no networking
docker run -it --rm --network none alpine sh

# Only loopback is available inside the container
/ # ip addr show
```

## 3. User-Defined Drivers

### 3.1 `bridge` (user-defined)
- When you create networks via Compose or `docker network create`, you get user-defined bridges.
- Benefits over `bridge` default network:
  - Container DNS support (service name resolution)
  - Fine-grained control over IP ranges, subnets, and gateway
- Example from our `docker-compose.yml`:

```yaml
networks:
  ecommerce-network:
    driver: bridge
```

### 3.2 `overlay` (Swarm/Kubernetes)
- **What**: A multi-host network that spans Swarm nodes.
- **Use case**: Required for services that run across multiple machines in a Swarm cluster.
- **How we use it**: In `docker-stack.yml`, we define `ecommerce-overlay` for multi-node Swarm deployments, allowing backend/front-end replicas to communicate across managers/workers.
- **Pros**:
  - Built-in encryption (optional) and secure cross-node communication
  - Works seamlessly with service discovery in Swarm
  - Supports rolling updates and scaling
- **Cons**:
  - Slight overhead vs single-host bridge networks
  - Requires Swarm mode
- **Example**:
```bash
# Initialize Swarm if not already active
docker swarm init --advertise-addr <manager-ip>

# Create an overlay network
docker network create --driver overlay --attachable global-net

# Deploy a service on the overlay network
docker service create --name demo-backend \
  --network global-net \
  --replicas 3 \
  <registry>/demo/backend:latest
```

### 3.3 `macvlan`
- **What**: Assigns unique MAC addresses to containers, making them appear as physical devices on the network.
- **Use case**: Integrating with existing VLANs where containers must be on the same network as VMs or physical hosts.
- **Relevance**: Typically not needed for the E-Commerce Platform but relevant for enterprise deployments that need direct L2 access or have strict IP management.
- **Pros**:
  - Direct L2 connectivity
  - Useful for legacy applications expecting unique MAC addresses
- **Cons**:
  - Complex networking setup (switch port configs, VLANs)
  - Difficult to use on laptops/virtualized environments
- **Example**:
```bash
# Substitute parent interface and subnet for your environment
docker network create -d macvlan \
  --subnet=192.168.1.0/24 \
  --gateway=192.168.1.1 \
  -o parent=enp0s31f6 \
  vlan-net

# Attach a container that appears directly on the LAN
docker run -d --name legacy-app --network vlan-net my-legacy:latest
```

### 3.4 `ipvlan`
- Similar to macvlan but relies on L3 IP routing rather than unique MAC addresses.
- Preferable in environments where macvlan is restricted.
- **Example**:
```bash
docker network create -d ipvlan \
  --subnet=10.10.0.0/24 \
  --gateway=10.10.0.1 \
  -o parent=eth0 \
  ipvlan-net
```

### 3.5 3rd-party or Plugin Drivers
- Vendors may provide specialized drivers (e.g., Weave, Calico, Flannel) for advanced segmentation, policy control, or integration with on-prem SDN solutions.

## 4. Networking in Our Project

### Compose (Local Development)
- **Network**: `ecommerce-network` (bridge)
- **Services**: Backend, frontend, PostgreSQL, Redis, Nginx share the user-defined bridge network using container names for DNS resolution.
- **Why**: Simplifies local development. All services communicate internally, while relevant ports are published to host for browser/API access.
- **Example Snippet**:
```yaml
services:
  backend:
    networks:
      - ecommerce-network
  frontend:
    networks:
      - ecommerce-network

networks:
  ecommerce-network:
    driver: bridge
```

### Swarm (Production-like)
- **Network**: `ecommerce-overlay` (overlay driver)
- **Services**: Backend, frontend, PostgreSQL, Redis, Nginx attach to the overlay network across multiple nodes.
- **Why**: Allows horizontal scaling, service discovery, and resilience. Overlay required for multi-node deployments and rolling updates.
- **Example Snippet**:
```yaml
services:
  backend:
    networks:
      - ecommerce-overlay
  frontend:
    networks:
      - ecommerce-overlay

networks:
  ecommerce-overlay:
    driver: overlay
```

### Additional Considerations
- Use `docker network inspect <network>` to view container attachments and IP addresses.
- Use network aliases to provide stable references to services if necessary.

## 5. When to Choose Each Driver

| Scenario | Recommended Driver | Notes |
| -------- | ------------------ | ----- |
| Local development on single machine | `bridge` | Use Compose-managed networks, rely on DNS names like `backend`, `postgres`. |
| Multi-service demo on a single server | `bridge` | Compose or Docker CLI is sufficient. |
| Production-like deployment across multiple nodes | `overlay` | Requires Swarm; use `docker stack deploy`. |
| Legacy integration requiring same network as host | `macvlan` | Ensure network gear supports it. |
| Performance testing without NAT | `host` | Only if port conflicts are manageable. |
| Security sandboxing / custom network setups | `none` | Optionally attach to networks later using `docker network connect`. |

## 6. Practical Commands

```bash
# List networks on host
docker network ls

# Inspect a specific network
docker network inspect ecommerce-network

# Create a new bridge network
docker network create --driver bridge analytics-network

# Connect an existing container to another network
docker network connect analytics-network devop-app_backend_1

# Disconnect a container
docker network disconnect ecommerce-network devop-app_backend_1

# Remove a network (no attached containers)
docker network rm analytics-network
```

## 7. Troubleshooting Networking Issues

- `docker ps` + `docker network inspect` to verify container is attached.
- `docker exec <container> ping <service>` to test reachability (install `iputils-ping` if missing).
- For Swarm overlay: ensure `docker network ls` shows overlay network and nodes are healthy (`docker node ls`).
- Check firewall rules: overlay networks need UDP 4789, TCP/UDP 7946 open between nodes.
- Use `docker events --filter 'type=network'` to track network-level events.

---

Keep this guide handy to choose appropriate Docker network drivers as the platform evolves from single-node demos to multi-node microservices deployments.
