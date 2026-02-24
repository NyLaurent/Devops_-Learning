# Docker Swarm Operations Guide

This guide walks through preparing, deploying, and operating the E-Commerce Platform on Docker Swarm, including scaling strategies, upgrades, and troubleshooting tips.

## 1. Prerequisites

- Docker Engine 24+ installed on all nodes.
- Swarm ports open between nodes (TCP/UDP 2377, 7946, UDP 4789).
- Access to a container registry that hosts the backend/frontend images (for example, GitLab Container Registry or Docker Hub).
- `docker-stack.yml` present in the repository root.
- Environment variables set for image tags (e.g., `BACKEND_IMAGE`, `FRONTEND_IMAGE`).

## 2. Initialize the Swarm

On the manager node:

```bash
docker swarm init --advertise-addr <manager-ip>
```

This outputs a worker join token. Join worker nodes with:

```bash
docker swarm join --token <worker-token> <manager-ip>:2377
```

To add manager nodes, use the manager join token (`docker swarm join-token manager`).

Verify cluster nodes:

```bash
docker node ls
```

## 3. Prepare Images

Build and push versioned images before deploying.

```bash
# Backend
cd backend
docker build -t <registry>/devop-app/backend:1.0.0 .
docker push <registry>/devop-app/backend:1.0.0

# Frontend
cd ../frontend
docker build -t <registry>/devop-app/frontend:1.0.0 .
docker push <registry>/devop-app/frontend:1.0.0
```

Export the image tags for stack deployment:

```bash
export BACKEND_IMAGE=<registry>/devop-app/backend:1.0.0
export FRONTEND_IMAGE=<registry>/devop-app/frontend:1.0.0
```

(Optional) set these variables via an `.env` file or in the CI pipeline.

## 4. Deploy the Stack

From the project root:

```bash
docker stack deploy -c docker-stack.yml devop-app
```

Monitor rollout:

```bash
docker stack services devop-app
# or verbose task view
docker stack ps devop-app
```

List running containers and published ports:

```bash
docker service ls
docker service ps devop-app_backend
```

Remove the stack when needed:

```bash
docker stack rm devop-app
```

## 5. Scaling Services

Swarm services can be scaled dynamically.

```bash
# Scale backend replicas to 4
docker service scale devop-app_backend=4

# Scale frontend back to 2 replicas
docker service scale devop-app_frontend=2
```

> **Note:** Stateful services like PostgreSQL/Redis should remain single replicas unless you have clustering/replication configured.

Verify distribution:

```bash
docker service ps devop-app_backend --no-trunc
```

## 6. Upgrading Services (Rolling Updates)

Update images and perform rolling upgrades without downtime.

```bash
# Push new images
 docker build -t <registry>/devop-app/backend:1.1.0 backend
 docker push <registry>/devop-app/backend:1.1.0

# Update the running service with new image
docker service update \
  --image <registry>/devop-app/backend:1.1.0 \
  --update-parallelism 1 \
  --update-delay 10s \
  devop-app_backend
```

The `docker-stack.yml` file already defines rolling update defaults (`update_config`) for backend/front-end services. To redeploy the entire stack with new tags:

```bash
export BACKEND_IMAGE=<registry>/devop-app/backend:1.1.0
export FRONTEND_IMAGE=<registry>/devop-app/frontend:1.1.0
docker stack deploy -c docker-stack.yml devop-app
```

Rollback to the previous version if issues arise:

```bash
docker service rollback devop-app_backend
```

## 7. Health Checks & Monitoring

Check service health:

```bash
docker service ls

# Inspect tasks and their current state
docker service ps devop-app_backend

# Check backend health endpoint directly
curl http://<manager-ip>:8080/actuator/health
```

Real-time stats and logs:

```bash
docker stats   # per-container CPU/memory usage
docker service logs -f devop-app_backend
```

For aggregated logging/monitoring consider hooking into ELK, Prometheus, or the built-in Docker logging drivers.

## 8. Troubleshooting

| Issue | Commands & Tips |
| ----- | ---------------- |
| Service stuck in `Pending` | Ensure enough Swarm resources (CPU/RAM/ports). Check `docker node ls` for availability. |
| Container restarting repeatedly | `docker service logs devop-app_backend` and inspect application stack trace. |
| Image pull errors | Verify registry credentials, login via `docker login`, and ensure image tags exist. |
| Network connectivity issues | `docker network inspect devop-app_ecommerce-overlay` to confirm overlay network is healthy. |
| Need to drain a node | `docker node update --availability drain <node>` to reschedule tasks elsewhere. |
| Remove stray resources | `docker system prune` (use with caution) or `docker volume ls` / `docker volume rm` for unused volumes. |

Additional quick commands:

```bash
# Inspect detailed service configuration
docker service inspect devop-app_backend --pretty

# Execute a command inside a running task
docker exec -it $(docker ps --filter "name=devop-app_backend" -q | head -n1) /bin/sh

# Check overlay network members
docker network inspect devop-app_ecommerce-overlay
```

## 9. High Availability Tips

- Maintain at least 3 manager nodes (odd number) to avoid losing quorum.
- Use labels and constraints if you need to pin services to specific node classes.
- Consider secrets/configs for environment variables instead of raw env settings (`docker secret create`, `docker config create`).
- Regularly back up persistent volumes (PostgreSQL). Use `docker run --rm --volumes-from` strategy or dedicated backup containers.

## 10. Removing Nodes

Drain and remove nodes gracefully:

```bash
docker node update --availability drain <node>

# After tasks migrate
 docker node rm <node>
```

If the manager node is being replaced, demote first (`docker node demote`).

## 11. Disaster Recovery Checklist

1. Back up database volumes and `.env` files.
2. Keep latest image tags documented.
3. Store `docker-stack.yml` and this guide in source control/knowledge base.
4. Recreate swarm quickly via `docker swarm init` and `docker stack deploy` using backed-up artifacts.

---

Keep this guide accessible to operations and instructional teams to ensure smooth deployments, scaling exercises, and upgrades when demonstrating microservices to students.

