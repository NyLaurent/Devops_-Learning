# Docker Compose Command Reference

This guide covers the day-to-day Docker Compose commands used to manage the E-Commerce Platform during local development, testing, and troubleshooting.

## Prerequisites

- Docker Engine ≥ 24
- Docker Compose v2 (`docker compose` CLI). For older environments replace `docker compose` with `docker-compose`.
- `.env` file present in the project root (create from `env.example` if missing).

---

## Core Lifecycle Commands

| Task | Command |
| ---- | ------- |
| Start all services (detached) | `docker compose up -d` |
| Start with rebuild | `docker compose up -d --build` |
| Start with specific profile | `docker compose --profile production up -d` |
| Stop containers (preserve data/volumes) | `docker compose stop` |
| Restart services | `docker compose restart` |
| Shut down and remove containers/networks | `docker compose down` |
| Reset everything (including volumes) | `docker compose down -v --remove-orphans` |

> **Tip:** Use `docker compose ps` to see running containers and their states at any time.

---

## Building Images

```bash
# Build all services defined in docker-compose.yml
docker compose build

# Build a specific service only
docker compose build backend
docker compose build frontend

# Build without using cache
docker compose build --no-cache backend
```

---

## Logs & Monitoring

```bash
# Tail logs for every container (follow mode)
docker compose logs -f

# Tail logs for a single service
docker compose logs -f backend

# Show only the last N lines
docker compose logs --tail=100 backend

# Timestamp log output
docker compose logs -t frontend
```

Health and status checks:

```bash
# List container status and ports
docker compose ps

# Run backend health endpoint manually
curl http://localhost:8080/actuator/health

# Inspect container stats (CPU, memory)
docker stats
```

---

## Database Operations (PostgreSQL)

```bash
# Connect to PostgreSQL via psql shell
docker compose exec postgres psql -U ecommerce -d ecommerce

# Review schema or run ad-hoc SQL
docker compose exec postgres psql -U ecommerce -d ecommerce -c "SELECT * FROM products LIMIT 5;"

# Dump database to host machine
docker compose exec postgres pg_dump -U ecommerce ecommerce > backups/db-$(date +%F).sql

# Restore from a local dump file
cat backups/db.sql | docker compose exec -T postgres psql -U ecommerce -d ecommerce
```

Database cleanup helpers:

```bash
# Drop and recreate database (data loss!)
docker compose exec postgres dropdb -U ecommerce ecommerce
 docker compose exec postgres createdb -U ecommerce ecommerce

# Remove all database volumes
docker compose down -v
```

---

## Running Backend Utilities

```bash
# Run Flyway migrations manually
docker compose exec backend ./mvnw flyway:migrate

# Execute Spring Boot tests inside container
docker compose exec backend ./mvnw test

# Open an interactive shell within the backend container
docker compose exec backend /bin/sh
```

---

## Frontend Utilities

```bash
# Install dependencies (if containerized for the first time)
docker compose exec frontend npm install

# Run unit tests inside frontend container
docker compose exec frontend npm test

# Build production assets manually
docker compose exec frontend npm run build
```

---

## Scaling Services

```bash
# Scale backend to 3 replicas (stateless services only)
docker compose up -d --scale backend=3

# Return to single replica
docker compose up -d --scale backend=1
```

> **Note:** Scaling stateful services (PostgreSQL, Redis) is not recommended in Compose unless you understand the ramifications.

---

## Troubleshooting Cheatsheet

| Symptom | Command / Action |
| ------- | ---------------- |
| Container crash loop | `docker compose logs <service>` — inspect stack trace |
| Ports already in use | `lsof -i :8080` or adjust published ports in `docker-compose.yml` |
| Database connection refused | `docker compose ps postgres` and review health check, ensure `.env` credentials match |
| Stale dependencies / build issues | `docker compose build --no-cache backend` |
| Persistent bad data | `docker compose down -v` (clears volumes) |
| Environment variable mismatches | `docker compose config` to render final merged config |
| Service startup order | `docker compose up -d postgres && docker compose up backend frontend` to force order |

Additional debugging commands:

```bash
# Inspect container environment variables
docker compose exec backend env

# View container file system
docker compose exec backend ls -al

# Copy file from container to host
docker cp $(docker compose ps -q backend):/app/logs/app.log logs/app.log
```

---

## Using Development Overrides

The project ships with `docker-compose.dev.yml` to enable live-reload workflows.

```bash
# Run base compose with dev overrides (adds watch volume mounts)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Stop and clean up dev stack
docker compose -f docker-compose.yml -f docker-compose.dev.yml down
```

---

## Environment Management

```bash
# Show effective configuration (env merged)
docker compose config

# Validate Compose file syntax
docker compose config --quiet && echo "config OK"

# Pass one-off environment variable
docker compose run -e SPRING_PROFILES_ACTIVE=qa backend
```

Remember to keep `.env` synchronized with credentials and ports when onboarding teammates.

---

## Cleaning Up Resources

```bash
# Remove dangling images
docker image prune

# Remove unused volumes (dangerous)
docker volume prune

# Remove every image, container, network (factory reset)
docker system prune -a
```

> Always back up database volumes before destructive cleanups using the dump commands above.

---

## Summary Workflow

1. `cp env.example .env` (first-time setup)
2. `docker compose up -d --build`
3. Verify with `docker compose ps` + `docker compose logs -f`
4. Access services:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - PostgreSQL: localhost:5432
5. Troubleshoot using the commands in this guide.
6. When finished: `docker compose down` (or `down -v` to reset data).

Keep this guide nearby to streamline daily operations and respond quickly to issues during development and demos.

