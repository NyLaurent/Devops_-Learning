# GitLab CI/CD Pipeline Documentation

## Overview

This project uses GitLab CI/CD for automated building, testing, and deployment of both backend and frontend services. Each service has its own separate pipeline configuration. All pipelines are configured to use a GitLab Runner with the tag `staging`.

## Pipeline Structure

The project has **separate CI/CD pipelines** for each codebase:

- **Backend Pipeline**: `backend/.gitlab-ci.yml` - Handles Java Spring Boot backend
- **Frontend Pipeline**: `frontend/.gitlab-ci.yml` - Handles React frontend
- **Root Pipeline**: `.gitlab-ci.yml` - Includes both pipelines using GitLab's `include` feature

Each pipeline runs independently and only triggers when changes are made to its respective directory.

## Pipeline Stages

Each service pipeline (backend and frontend) consists of the following stages:

1. **Build** - Compiles/builds the application
2. **Test** - Runs unit and integration tests
3. **Build Docker** - Builds and pushes Docker image to container registry
4. **Deploy** - Deploys the service to staging environment

### Backend Stages
- Build: Compiles Java Spring Boot application using Maven
- Test: Runs unit and integration tests with PostgreSQL service
- Build Docker: Creates Docker image from backend Dockerfile
- Deploy: Deploys to staging (Docker Compose or Kubernetes)

### Frontend Stages
- Build: Builds React application using npm
- Test: Runs React tests and generates coverage reports
- Build Docker: Creates Docker image with Nginx serving static files
- Deploy: Deploys to staging (Docker Compose or Kubernetes)

## Pipeline Configuration

### Runner Configuration

The pipeline uses a GitLab Runner with the tag `staging`. To configure a runner with this tag:

1. Register the runner:
```bash
gitlab-runner register
```

2. When prompted, provide:
   - GitLab URL: `https://gitlab.com/` (or your GitLab instance URL)
   - Registration token: (from GitLab project Settings > CI/CD > Runners)
   - Description: `staging-runner`
   - Tags: `staging`
   - Executor: `docker` (recommended) or `shell`

3. For Docker executor, ensure Docker is installed and the runner has permissions:
```bash
# Add gitlab-runner to docker group
sudo usermod -aG docker gitlab-runner
```

### Required GitLab CI/CD Variables

Configure the following variables in GitLab (Settings > CI/CD > Variables):

| Variable | Description | Example |
|----------|-------------|---------|
| `CI_REGISTRY` | Container registry URL | `registry.gitlab.com` |
| `CI_REGISTRY_USER` | Registry username | `gitlab-ci-token` |
| `CI_REGISTRY_PASSWORD` | Registry password | `$CI_JOB_TOKEN` |
| `CI_REGISTRY_IMAGE` | Registry image path | `registry.gitlab.com/group/project` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `ROLLBACK_IMAGE_TAG` | Image tag for rollback (set manually) | - |

## Pipeline Jobs

### Backend Jobs

#### 1. build:backend

**Stage:** `build`  
**Runner Tag:** `staging`  
**Image:** `maven:3.9.9-eclipse-temurin-21`

- Compiles the Java application using Maven
- Caches Maven dependencies for faster builds
- Creates build artifacts (JAR files)

**Triggers:**
- Changes to `backend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches
- Merge requests

#### 2. test:backend

**Stage:** `test`  
**Runner Tag:** `staging`  
**Image:** `maven:3.9.9-eclipse-temurin-21`  
**Services:** `postgres:15-alpine`

- Runs unit and integration tests
- Requires PostgreSQL database service
- Generates test reports and coverage reports
- Artifacts are stored for 1 week

**Triggers:**
- Changes to `backend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches
- Merge requests

#### 3. build-docker:backend

**Stage:** `build-docker`  
**Runner Tag:** `staging`  
**Image:** `docker:24-dind`

- Builds Docker image from the backend Dockerfile
- Tags image with:
  - `$CI_COMMIT_SHORT_SHA` - Short commit hash
  - `latest` - Latest tag
  - `$CI_COMMIT_REF_SLUG` - Branch name (sanitized)
- Pushes images to GitLab Container Registry as `$CI_REGISTRY_IMAGE/backend:*`

**Triggers:**
- Changes to `backend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches

#### 4. deploy:backend:staging

**Stage:** `deploy`  
**Runner Tag:** `staging`  
**Image:** `alpine:latest`

- Deploys the backend to staging environment
- Supports two deployment methods:
  - **Docker Compose**: Updates `docker-compose.yml` and deploys
  - **Kubernetes**: Updates `k8s/backend/deployment.yaml` and applies deployment
- Performs health checks after deployment

**Triggers:**
- Changes to `backend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches (automatic on success)

**Environment:** `staging/backend`

#### 5. rollback:backend:staging

**Stage:** `deploy`  
**Runner Tag:** `staging`  
**When:** `manual`

- Allows manual rollback to a previous image version
- Requires `ROLLBACK_IMAGE_TAG` variable to be set

---

### Frontend Jobs

#### 1. build:frontend

**Stage:** `build`  
**Runner Tag:** `staging`  
**Image:** `node:18-alpine`

- Installs npm dependencies
- Builds React application using `npm run build`
- Caches node_modules for faster builds
- Creates build artifacts (build/ directory)

**Triggers:**
- Changes to `frontend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches
- Merge requests

#### 2. test:frontend

**Stage:** `test`  
**Runner Tag:** `staging`  
**Image:** `node:18-alpine`

- Runs React tests using Jest
- Generates test coverage reports
- Artifacts are stored for 1 week

**Triggers:**
- Changes to `frontend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches
- Merge requests

#### 3. build-docker:frontend

**Stage:** `build-docker`  
**Runner Tag:** `staging`  
**Image:** `docker:24-dind`

- Builds Docker image from the frontend Dockerfile
- Multi-stage build: Node.js for build, Nginx for serving
- Tags image with:
  - `$CI_COMMIT_SHORT_SHA` - Short commit hash
  - `latest` - Latest tag
  - `$CI_COMMIT_REF_SLUG` - Branch name (sanitized)
- Pushes images to GitLab Container Registry as `$CI_REGISTRY_IMAGE/frontend:*`

**Triggers:**
- Changes to `frontend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches

#### 4. deploy:frontend:staging

**Stage:** `deploy`  
**Runner Tag:** `staging`  
**Image:** `alpine:latest`

- Deploys the frontend to staging environment
- Supports two deployment methods:
  - **Docker Compose**: Updates `docker-compose.yml` and deploys
  - **Kubernetes**: Updates `k8s/frontend/deployment.yaml` and applies deployment
- Performs health checks after deployment

**Triggers:**
- Changes to `frontend/**/*` or root `.gitlab-ci.yml`
- Push to `main` or `develop` branches (automatic on success)

**Environment:** `staging/frontend`

#### 5. rollback:frontend:staging

**Stage:** `deploy`  
**Runner Tag:** `staging`  
**When:** `manual`

- Allows manual rollback to a previous image version
- Requires `ROLLBACK_IMAGE_TAG` variable to be set

---

### Rollback Usage

1. Go to GitLab CI/CD > Pipelines
2. Click on a pipeline
3. Click the play button (▶) on the `rollback:backend:staging` or `rollback:frontend:staging` job
4. Set the `ROLLBACK_IMAGE_TAG` variable (e.g., `abc1234`) when prompted

## Deployment Methods

### Docker Compose Deployment

If `docker-compose.yml` exists in the project root, the pipeline will:

1. Update the image tag in `docker-compose.yml`
2. Pull the new image
3. Start the backend service
4. Wait for health check endpoint

**Requirements:**
- Docker and Docker Compose must be installed on the runner
- Runner must have permissions to use Docker

### Kubernetes Deployment

If `k8s/backend/deployment.yaml` exists, the pipeline will:

1. Update the image tag in the deployment YAML
2. Apply the Kubernetes deployment
3. Wait for rollout to complete
4. Check deployment status

**Requirements:**
- `kubectl` must be installed on the runner
- Kubernetes configuration file (`~/.kube/config`) must be available
- Appropriate RBAC permissions for the service account

**Setup Kubernetes Access:**
```bash
# On the runner machine
mkdir -p ~/.kube
cp /path/to/kubeconfig ~/.kube/config
chmod 600 ~/.kube/config
```

## Health Checks

The pipeline performs health checks after deployment:

- **Docker Compose**: Checks `http://localhost:8080/actuator/health`
- **Kubernetes**: Uses `kubectl wait` to check deployment availability

If health checks fail, the deployment job fails.

## Troubleshooting

### Runner Not Picking Up Jobs

1. Check runner status:
```bash
gitlab-runner status
```

2. Verify runner tags match:
```bash
gitlab-runner verify
```

3. Check runner configuration:
```bash
cat /etc/gitlab-runner/config.toml
```

### Docker Build Failures

1. Ensure Docker daemon is running:
```bash
sudo systemctl status docker
```

2. Check Docker permissions:
```bash
docker ps
```

3. Verify Docker-in-Docker (dind) service is configured correctly

### Kubernetes Deployment Failures

1. Verify `kubectl` is configured:
```bash
kubectl cluster-info
```

2. Check namespace exists:
```bash
kubectl get namespace ecommerce
```

3. Verify image pull secrets are configured:
```bash
kubectl get secrets
```

### Test Failures

1. Check PostgreSQL service is available:
```bash
docker ps | grep postgres
```

2. Verify test database credentials in job variables
3. Check test logs in GitLab CI/CD job output

## Best Practices

1. **Use Branch Protection**: Protect `main` and `develop` branches to require MR approval
2. **Monitor Deployments**: Set up alerts for failed deployments
3. **Keep Secrets Secure**: Use GitLab CI/CD variables for sensitive data, not commit them
4. **Regular Updates**: Keep Docker images and dependencies updated
5. **Review Logs**: Regularly review pipeline logs for performance and errors
6. **Test Locally**: Test Docker builds and deployments locally before pushing

## Local Testing

### Backend Testing

#### Test Backend Docker Build Locally

```bash
cd backend
docker build -t backend:test .
```

#### Test Backend Docker Compose Locally

```bash
docker-compose up -d backend
curl http://localhost:8080/actuator/health
```

#### Test Backend Kubernetes Deployment Locally

```bash
kubectl apply -f k8s/backend/deployment.yaml
kubectl rollout status deployment/backend -n ecommerce
```

### Frontend Testing

#### Test Frontend Build Locally

```bash
cd frontend
npm ci
npm run build
```

#### Test Frontend Docker Build Locally

```bash
cd frontend
docker build -t frontend:test .
```

#### Test Frontend Docker Compose Locally

```bash
docker-compose up -d frontend
curl http://localhost:3000
```

#### Test Frontend Kubernetes Deployment Locally

```bash
kubectl apply -f k8s/frontend/deployment.yaml
kubectl rollout status deployment/frontend -n ecommerce
```

## Pipeline Visualization

View pipeline visualization in GitLab:
- Go to CI/CD > Pipelines
- Click on a pipeline
- View the pipeline graph

## Additional Resources

- [GitLab CI/CD Documentation](https://docs.gitlab.com/ee/ci/)
- [GitLab Runner Documentation](https://docs.gitlab.com/runner/)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)

