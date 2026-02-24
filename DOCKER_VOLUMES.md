# Docker Volumes & Mounts Explained

Persistent storage is essential for databases, logs, and application state. This guide covers the storage options available in Docker, how they differ, and when to use each in the E-Commerce Platform.

## 1. Storage Basics

- **Volume**: Managed by Docker, stored in `/var/lib/docker/volumes` (by default). Decoupled from container lifecycle.
- **Bind Mount**: Maps a file or directory on the host filesystem to the container. Updates reflect both ways immediately.
- **tmpfs Mount**: An in-memory filesystem. Fast but ephemeral, data disappears when container stops.
- **Named vs Anonymous**: Volumes can be named (recommended) or anonymous. Named volumes are easier to inspect, back up, or share across services.

## 2. Storage Types & Drivers

| Type | Definition | Typical Use | Pros | Cons |
| ---- | ---------- | ----------- | ---- | ---- |
| **Named Volume** | Managed by Docker with a human-readable name | Database data, shared state across container restarts | Decoupled from host path, easy backups | Stored in Docker-managed path (less direct control) |
| **Anonymous Volume** | Automatically created without a name | Temporary persistence when no name provided | Automatic creation | Hard to track, clean up |
| **Bind Mount** | Host path mapped into container | Source code, configuration files, local development | Full control, instant changes | Host path must exist, less portable |
| **tmpfs** | In-memory mount | Sensitive data, caching, tests | Extremely fast, data never hits disk | Data lost on stop, consumes RAM |
| **NFS/SMB Volume (plugin)** | Remote network filesystem | Shared storage across hosts | Centralized storage, multi-host | Requires external infrastructure |
| **Local Volume with Driver Options** | Local driver with customization (e.g., device, size) | Advanced storage needs (RAID partitions) | Control over driver options | More complex setup |

## 3. How We Use Storage in This Project

### docker-compose.yml (Local Development)

```yaml
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
```

- `postgres_data`: Stores PostgreSQL data files. Persisted across `docker compose up/down`.
- `redis_data`: Optional caching state (can be pruned without major impact).
- `./backend/logs:/app/logs`: Bind mount for easy log inspection in development.
- `./backend/src:/app/src`: Used in `docker-compose.dev.yml` for live reload (bind mount).

### docker-stack.yml (Swarm)

```yaml
volumes:
  postgres_data:
  backend_logs:
  redis_data:
```

- Swarm volumes behave similarly to local volumes but are managed cluster-wide. If you use Docker Swarm across multiple nodes, ensure the underlying storage is accessible (NFS, cloud volumes, or bind to node-specific storage with placement constraints).

### Development Overrides

`docker-compose.dev.yml` uses bind mounts to sync source code changes into containers for hot reload in both backend and frontend services.

## 4. When to Choose Each Option

### Named Volumes
- **Use when**: You need persistence without caring about the host path (e.g., databases).
- **Benefits**:
  - Portable between environments
  - Docker handles lifecycle
  - Easy to back up via `docker run --rm --volumes-from`
- **Drawbacks**:
  - Stored in Docker-managed directories; direct access requires `docker volume inspect`

### Bind Mounts
- **Use when**: You need direct control over host files (development, logs, certificates, static assets).
- **Benefits**:
  - Immediate reflection of host changes inside container
  - Works well for local dev where a text editor is outside container
- **Drawbacks**:
  - Host path must exist
  - Less portable; scripts must create expected directories
  - Permissions can be tricky on Linux (use `chown` or `userns-remap`)

### tmpfs
- **Use when**: You require ultra-fast storage and do not need data after container stops (e.g., caching, tests, sensitive secrets).
- **Benefits**:
  - Lives in RAM; never written to disk
  - Secure (data cleared on stop)
- **Drawbacks**:
  - Data lost after container stops
  - Consumes RAM; may affect host memory usage

### Networked Volumes (NFS, SMB)
- **Use when**: Multiple nodes need to share the same volume data.
- **Benefits**:
  - Centralized storage for multi-node deployments
- **Drawbacks**:
  - Needs external infrastructure and careful configuration

## 5. Commands & Workflows

### Inspect Volumes
```bash
# List volumes
docker volume ls

# Inspect a named volume
docker volume inspect postgres_data
```

### Back Up & Restore Named Volumes
```bash
# Backup using a temporary busybox container
docker run --rm \
  --volumes-from $(docker compose ps -q postgres) \
  -v $(pwd)/backups:/backup \
  busybox tar cvf /backup/postgres-backup.tar /var/lib/postgresql/data

# Restore
docker run --rm \
  --volumes-from $(docker compose ps -q postgres) \
  -v $(pwd)/backups:/backup \
  busybox sh -c "cd /var/lib/postgresql/data && tar xvf /backup/postgres-backup.tar --strip 1"
```

### Removing Volumes
```bash
# Remove unused volumes
docker volume prune

# Remove specific named volume
docker volume rm postgres_data
```

### Bind Mount Examples
```yaml
services:
  backend:
    volumes:
      - ./backend/logs:/app/logs          # log files
      - ./backend/src:/app/src            # live reload (dev override)
```

### tmpfs Example
```yaml
services:
  backend:
    tmpfs:
      - /tmp/cache
```

### Restricting Permissions
For Linux hosts, ensure UID/GID matches container expectations. Example for PostgreSQL (UID 70):

```bash
sudo chown -R 70:70 $(docker volume inspect postgres_data -f '{{ .Mountpoint }}')
```

## 6. Choosing Strategies for the Platform

| Component | Development | Swarm Production | Notes |
| --------- | ----------- | ---------------- | ----- |
| PostgreSQL | Named volume (`postgres_data`) | Named volume (`postgres_data`, possibly backed by NFS / cloud block storage) | Back up regularly |
| Backend logs | Bind mount (`./backend/logs`) | Named volume (`backend_logs`) or log driver | In production, consider centralized logging |
| Frontend build | Container image only | Container image only | No persistent data required |
| Redis | Named volume (`redis_data`) | Named volume (if persistence needed) | Redis often disposable; volume optional |

## 7. Troubleshooting Storage Issues

- **Volume not mounting**: Ensure volume exists (`docker volume ls`). For bind mounts, verify host path.
- **Permission errors**: Match host directory permissions to container user. Use `chown` or Docker Compose `user:` settings.
- **Volume data not updating**: For bind mounts, confirm the host file is saved; for named volumes, remember they are decoupled from host path.
- **Clean slate needed**: `docker compose down -v` removes containers and volumes (data loss!).
- **Swarm volume scheduling**: Ensure volume drivers support multi-node use. For local driver, pin the service to a node via placement constraints.

## 8. Best Practices

1. **Name your volumes**: Avoid anonymous volumes that linger unused.
2. **Keep development and production strategies parallel**: Use named volumes locally to mimic production persistence behavior.
3. **Back up critical data**: Use scripts/cron to tar named volume mount points.
4. **Avoid bind mounts in production** unless necessary; they couple deployment to host paths.
5. **Document volume usage**: Identify which services rely on persistent volumes to aid onboarding.

---

Use this guide to select the right storage strategy for each service, ensuring data durability during demos and real deployments while keeping development workflows flexible.

