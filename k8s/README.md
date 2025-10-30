# Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying the Agent Application.

## Prerequisites

- Kubernetes cluster (v1.20+)
- kubectl configured
- Kustomize (v4.0+)
- NGINX Ingress Controller
- Cert-Manager (for TLS certificates)
- Prometheus Operator (for monitoring)

## Quick Start

### 1. Create Namespace

```bash
kubectl apply -f namespace.yaml
```

### 2. Deploy Base Configuration

```bash
# Deploy all resources
kubectl apply -k .

# Or deploy individual components
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f agent-app-deployment.yaml
```

### 3. Verify Deployment

```bash
# Check pod status
kubectl get pods -n agent-application

# Check services
kubectl get services -n agent-application

# Check ingress
kubectl get ingress -n agent-application
```

## Environment-Specific Deployments

### Development

```bash
kubectl apply -k overlays/development/
```

### Production

```bash
kubectl apply -k overlays/production/
```

## Configuration

### Secrets

Update the secrets in `secrets.yaml` with base64 encoded values:

```bash
# Encode a secret
echo -n "your_secret_value" | base64

# Decode a secret
echo "eW91cl9zZWNyZXRfdmFsdWU=" | base64 -d
```

Required secrets:
- `db-username`: Database username
- `db-password`: Database password
- `redis-password`: Redis password
- `jwt-secret`: JWT signing secret
- `openai-api-key`: OpenAI API key (optional)
- `anthropic-api-key`: Anthropic API key (optional)

### ConfigMap

The application configuration is stored in `configmap.yaml`. Modify the `application.yml` section to update Spring Boot configuration.

### Ingress

Update the host in `ingress.yaml`:

```yaml
spec:
  rules:
  - host: your-domain.com  # Change this
```

## Monitoring

### Prometheus Metrics

The application exposes metrics at `/api/actuator/prometheus`. These are automatically scraped by Prometheus if you have the Prometheus Operator installed.

### Health Checks

- Liveness: `/api/actuator/health/liveness`
- Readiness: `/api/actuator/health/readiness`
- Startup: `/api/actuator/health`

### Alerts

PrometheusRule in `monitoring.yaml` defines alerts for:
- Application down
- High error rate
- High response time
- High memory/CPU usage
- Database/Redis down

## Scaling

### Manual Scaling

```bash
# Scale application pods
kubectl scale deployment agent-app --replicas=5 -n agent-application
```

### Auto Scaling

HPA is configured in `hpa.yaml` with:
- Min replicas: 2 (dev: 1, prod: 3)
- Max replicas: 10 (prod: 20)
- CPU target: 70% (prod: 60%)
- Memory target: 80% (prod: 70%)

## Security

### Network Policies

Network policies in `network-policy.yaml` restrict traffic:
- Agent app can access PostgreSQL and Redis
- Only ingress controller can access agent app
- External API access allowed for LLM providers

### RBAC

Service account with minimal permissions:
- Read ConfigMaps and Secrets
- Read own pod information
- Read services for discovery

### Pod Security

- Non-root user (UID 1001)
- Read-only root filesystem (where possible)
- Dropped capabilities
- Security context constraints

## Storage

### Persistent Volumes

- PostgreSQL: 10Gi (dev: 5Gi, prod: 50Gi)
- Redis: 5Gi (dev: 2Gi, prod: 10Gi)

### Storage Classes

- Default: `standard`
- Production: `fast-ssd` (configure based on your cluster)

## Troubleshooting

### Common Issues

1. **ImagePullBackOff**: Build and push the Docker image
2. **CrashLoopBackOff**: Check logs and resource limits
3. **Service Unavailable**: Check ingress and service configuration

### Debugging Commands

```bash
# Check pod logs
kubectl logs -f deployment/agent-app -n agent-application

# Describe pod for events
kubectl describe pod <pod-name> -n agent-application

# Check resource usage
kubectl top pods -n agent-application

# Port forward for local access
kubectl port-forward service/agent-app-service 8080:80 -n agent-application
```

### Database Connection Issues

```bash
# Test PostgreSQL connection
kubectl exec -it deployment/postgres -n agent-application -- psql -U agent -d agentdb -c "SELECT 1;"

# Test Redis connection
kubectl exec -it deployment/redis -n agent-application -- redis-cli ping
```

## Backup and Recovery

### Database Backup

```bash
# Create backup
kubectl exec deployment/postgres -n agent-application -- pg_dump -U agent agentdb > backup.sql

# Restore backup
kubectl exec -i deployment/postgres -n agent-application -- psql -U agent agentdb < backup.sql
```

### Redis Backup

```bash
# Create backup
kubectl exec deployment/redis -n agent-application -- redis-cli BGSAVE

# Copy backup file
kubectl cp agent-application/redis-pod:/data/dump.rdb ./redis-backup.rdb
```

## Updates and Rollbacks

### Rolling Updates

```bash
# Update image
kubectl set image deployment/agent-app agent-app=agent-application:v1.1.0 -n agent-application

# Check rollout status
kubectl rollout status deployment/agent-app -n agent-application
```

### Rollbacks

```bash
# View rollout history
kubectl rollout history deployment/agent-app -n agent-application

# Rollback to previous version
kubectl rollout undo deployment/agent-app -n agent-application

# Rollback to specific revision
kubectl rollout undo deployment/agent-app --to-revision=2 -n agent-application
```

## Cleanup

```bash
# Delete all resources
kubectl delete -k .

# Or delete namespace (removes everything)
kubectl delete namespace agent-application
```

## Production Checklist

- [ ] Update secrets with strong passwords
- [ ] Configure proper ingress hostname
- [ ] Set up TLS certificates
- [ ] Configure monitoring and alerting
- [ ] Set resource limits and requests
- [ ] Configure backup strategy
- [ ] Test disaster recovery procedures
- [ ] Configure log aggregation
- [ ] Set up network policies
- [ ] Configure pod security policies
- [ ] Test scaling scenarios