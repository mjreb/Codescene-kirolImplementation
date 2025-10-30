# Docker Deployment Guide

This directory contains Docker configuration files for the Agent Application.

## Quick Start

### Development Environment

1. **Start all services:**
   ```bash
   docker-compose up -d
   ```

2. **View logs:**
   ```bash
   docker-compose logs -f agent-app
   ```

3. **Stop services:**
   ```bash
   docker-compose down
   ```

### Production Environment

1. **Create environment file:**
   ```bash
   cp .env.example .env
   # Edit .env with your production values
   ```

2. **Start production services:**
   ```bash
   docker-compose -f docker-compose.prod.yml up -d
   ```

## Services

### Core Services

- **agent-app**: Main Spring Boot application (port 8080)
- **postgres**: PostgreSQL database (port 5432)
- **redis**: Redis cache (port 6379)

### Optional Services

- **ollama**: Local LLM service (port 11434)
- **zipkin**: Distributed tracing (port 9411)
- **prometheus**: Metrics collection (port 9090)
- **grafana**: Metrics visualization (port 3000)

## Configuration

### Environment Variables

#### Required for Production

- `JWT_SECRET`: JWT signing secret (minimum 256 bits)
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `REDIS_PASSWORD`: Redis password

#### Optional

- `OPENAI_API_KEY`: OpenAI API key
- `ANTHROPIC_API_KEY`: Anthropic API key
- `GOOGLE_CLIENT_ID`: Google OAuth client ID
- `GOOGLE_CLIENT_SECRET`: Google OAuth client secret
- `GITHUB_CLIENT_ID`: GitHub OAuth client ID
- `GITHUB_CLIENT_SECRET`: GitHub OAuth client secret

### Health Checks

All services include health checks:

- **Application**: `http://localhost:8080/api/actuator/health`
- **PostgreSQL**: `pg_isready` command
- **Redis**: `redis-cli ping`
- **Ollama**: `curl http://localhost:11434/api/tags`

### Monitoring

#### Prometheus Metrics

Available at: `http://localhost:8080/api/actuator/prometheus`

Key metrics:
- `http_server_requests_*`: HTTP request metrics
- `agent_conversations_*`: Conversation metrics
- `agent_llm_*`: LLM provider metrics
- `agent_tools_*`: Tool execution metrics

#### Grafana Dashboards

Access Grafana at: `http://localhost:3000`
- Username: `admin`
- Password: `admin123` (development) or `${GRAFANA_ADMIN_PASSWORD}` (production)

#### Distributed Tracing

Access Zipkin at: `http://localhost:9411`

## Volumes

### Persistent Data

- `postgres_data`: PostgreSQL data
- `redis_data`: Redis data
- `ollama_data`: Ollama models
- `prometheus_data`: Prometheus metrics
- `grafana_data`: Grafana dashboards and settings

### Backup

```bash
# Backup PostgreSQL
docker-compose exec postgres pg_dump -U agent agentdb > backup.sql

# Backup Redis
docker-compose exec redis redis-cli --rdb /data/dump.rdb
```

## Troubleshooting

### Common Issues

1. **Port conflicts**: Change ports in docker-compose.override.yml
2. **Memory issues**: Adjust JVM settings in Dockerfile
3. **Database connection**: Check PostgreSQL health and credentials
4. **Redis connection**: Verify Redis password and connectivity

### Logs

```bash
# Application logs
docker-compose logs -f agent-app

# Database logs
docker-compose logs -f postgres

# All services
docker-compose logs -f
```

### Debug Mode

Enable debug mode in development:

```bash
# Set environment variable
SPRING_PROFILES_ACTIVE=development,docker docker-compose up
```

## Security

### Production Checklist

- [ ] Change default passwords
- [ ] Use strong JWT secret
- [ ] Enable HTTPS (configure reverse proxy)
- [ ] Restrict network access
- [ ] Enable audit logging
- [ ] Configure backup strategy
- [ ] Set up monitoring alerts

### Network Security

The application uses a custom bridge network (`agent-network`) to isolate services.

### Secrets Management

For production, consider using:
- Docker Secrets
- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault

## Scaling

### Horizontal Scaling

```bash
# Scale application instances
docker-compose up -d --scale agent-app=3
```

### Resource Limits

Production configuration includes resource limits:
- CPU: 1.0 cores max, 0.5 cores reserved
- Memory: 1GB max, 512MB reserved

## Maintenance

### Updates

```bash
# Pull latest images
docker-compose pull

# Restart with new images
docker-compose up -d --force-recreate
```

### Cleanup

```bash
# Remove unused images
docker image prune -f

# Remove unused volumes
docker volume prune -f

# Complete cleanup
docker system prune -af
```