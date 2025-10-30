# Deployment Testing Scripts

This directory contains scripts and configurations for testing the deployment of the Agent Application.

## Scripts

### test-deployment.sh

Comprehensive deployment testing script that validates:
- Docker image building and container startup
- Docker Compose multi-service deployment
- Kubernetes deployment (if kubectl is available)
- API documentation generation and accuracy
- Basic performance testing

#### Usage

```bash
# Run all tests
./scripts/test-deployment.sh

# Skip Kubernetes tests
./scripts/test-deployment.sh --skip-k8s

# Skip Docker tests
./scripts/test-deployment.sh --skip-docker

# Skip performance tests
./scripts/test-deployment.sh --skip-perf

# Show help
./scripts/test-deployment.sh --help
```

#### Prerequisites

- Docker and Docker Compose
- Maven 3.6+
- Java 17+
- kubectl (optional, for Kubernetes tests)
- curl (for HTTP testing)

#### Environment Variables

- `RUN_DOCKER_TESTS=true`: Enable Docker deployment tests
- `RUN_K8S_TESTS=true`: Enable Kubernetes deployment tests

## Test Categories

### 1. Docker Tests

**File**: `src/test/java/com/agent/deployment/DockerDeploymentTest.java`

Tests Docker containerization:
- Application startup in container
- Database connectivity (PostgreSQL)
- Cache connectivity (Redis)
- Health check endpoints
- Prometheus metrics exposure
- Resource usage validation

**Prerequisites**:
- Docker and Docker Compose running
- Set environment variable: `RUN_DOCKER_TESTS=true`

**Execution**:
```bash
RUN_DOCKER_TESTS=true mvn test -Dtest=DockerDeploymentTest
```

### 2. Kubernetes Tests

**File**: `src/test/java/com/agent/deployment/KubernetesDeploymentTest.java`

Tests Kubernetes deployment:
- Namespace and resource existence
- Deployment readiness and pod health
- Service and endpoint configuration
- ConfigMap and Secret validation
- Resource limits and probes
- Health endpoint accessibility

**Prerequisites**:
- kubectl configured with test cluster access
- Test namespace: `agent-application-test`
- Application deployed to test namespace
- Set environment variable: `RUN_K8S_TESTS=true`

**Setup**:
```bash
# Create test namespace
kubectl create namespace agent-application-test

# Deploy application
kubectl apply -k k8s/overlays/development -n agent-application-test

# Run tests
RUN_K8S_TESTS=true mvn test -Dtest=KubernetesDeploymentTest
```

### 3. API Documentation Tests

**File**: `src/test/java/com/agent/deployment/ApiDocumentationTest.java`

Tests API documentation:
- OpenAPI specification generation
- Swagger UI accessibility
- Documentation completeness
- Schema validation
- Example accuracy
- Static file consistency

**Execution**:
```bash
mvn test -Dtest=ApiDocumentationTest
```

## CI/CD Integration

### GitHub Actions

**File**: `.github/workflows/deployment-tests.yml`

Automated deployment testing pipeline with jobs:

1. **docker-tests**: Docker image building and container testing
2. **kubernetes-tests**: Kind cluster deployment testing
3. **api-documentation-tests**: Documentation validation
4. **security-tests**: Container security scanning
5. **performance-tests**: Basic load testing
6. **integration-tests**: End-to-end testing with Postman

#### Triggers

- Push to `main` or `develop` branches
- Pull requests to `main`
- Manual workflow dispatch

#### Artifacts

- API documentation files
- Newman test results
- Security scan reports

## Local Testing

### Quick Start

1. **Build and test Docker deployment**:
   ```bash
   # Build application
   mvn clean package -DskipTests
   
   # Build Docker image
   docker build -t agent-application:test .
   
   # Test with Docker Compose
   docker-compose up -d
   sleep 60
   RUN_DOCKER_TESTS=true mvn test -Dtest=DockerDeploymentTest
   docker-compose down -v
   ```

2. **Test API documentation**:
   ```bash
   mvn test -Dtest=ApiDocumentationTest
   ```

3. **Test Kubernetes deployment** (requires cluster):
   ```bash
   # Deploy to test namespace
   kubectl create namespace agent-application-test
   kubectl apply -k k8s/overlays/development -n agent-application-test
   
   # Wait for deployment
   kubectl wait --for=condition=available --timeout=300s deployment/agent-app -n agent-application-test
   
   # Run tests
   RUN_K8S_TESTS=true mvn test -Dtest=KubernetesDeploymentTest
   
   # Cleanup
   kubectl delete namespace agent-application-test
   ```

### Manual Testing

#### Docker

```bash
# Build and run
docker build -t agent-application:manual .
docker run -d --name test-app -p 8080:8080 agent-application:manual

# Test endpoints
curl http://localhost:8080/api/actuator/health
curl http://localhost:8080/api/swagger-ui.html

# Cleanup
docker stop test-app && docker rm test-app
```

#### Docker Compose

```bash
# Start services
docker-compose up -d

# Test application
curl http://localhost:8080/api/actuator/health
curl http://localhost:8080/api/v3/api-docs

# View logs
docker-compose logs agent-app

# Cleanup
docker-compose down -v
```

#### Kubernetes

```bash
# Apply manifests
kubectl apply -k k8s/

# Check status
kubectl get pods -n agent-application
kubectl get services -n agent-application

# Port forward for testing
kubectl port-forward service/agent-app-service 8080:80 -n agent-application

# Test endpoints
curl http://localhost:8080/api/actuator/health

# Cleanup
kubectl delete -k k8s/
```

## Troubleshooting

### Common Issues

1. **Docker build fails**:
   - Check Java version (requires 17+)
   - Ensure Maven build succeeds first
   - Verify Dockerfile syntax

2. **Container startup fails**:
   - Check application logs: `docker logs <container-id>`
   - Verify environment variables
   - Check resource limits

3. **Kubernetes deployment fails**:
   - Check pod status: `kubectl describe pod <pod-name>`
   - Verify ConfigMap and Secret values
   - Check resource quotas and limits

4. **Tests fail**:
   - Ensure prerequisites are met
   - Check environment variables
   - Verify network connectivity

### Debug Commands

```bash
# Docker debugging
docker logs <container-id>
docker exec -it <container-id> /bin/sh
docker inspect <container-id>

# Kubernetes debugging
kubectl describe deployment agent-app -n agent-application
kubectl logs -l app.kubernetes.io/name=agent-application -n agent-application
kubectl get events -n agent-application

# Test debugging
mvn test -Dtest=DockerDeploymentTest -X
mvn test -Dtest=KubernetesDeploymentTest -Dspring.profiles.active=test
```

## Performance Considerations

### Resource Requirements

- **Minimum**: 512MB RAM, 0.5 CPU cores
- **Recommended**: 1GB RAM, 1 CPU core
- **Database**: PostgreSQL with 256MB RAM
- **Cache**: Redis with 128MB RAM

### Scaling

- Horizontal scaling supported via Kubernetes HPA
- Database connection pooling configured
- Stateless application design

### Monitoring

- Health checks on `/actuator/health`
- Metrics on `/actuator/prometheus`
- Distributed tracing with Zipkin
- Structured logging with correlation IDs

## Security Testing

### Container Security

- Non-root user execution
- Minimal base image (Alpine)
- No sensitive files in image
- Security scanning with Trivy

### Application Security

- JWT token validation
- API key authentication
- Input validation
- Rate limiting
- CORS configuration

## Maintenance

### Regular Tasks

1. Update base images monthly
2. Review and update dependencies
3. Run security scans
4. Performance testing
5. Documentation updates

### Monitoring

- Set up alerts for deployment failures
- Monitor resource usage trends
- Track deployment success rates
- Review security scan results