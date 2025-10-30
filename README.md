# Agent Application

An intelligent agent application implementing the ReAct (Reasoning and Acting) pattern for conversational AI interactions. The application provides a robust platform for creating and managing conversations with AI agents that can reason about tasks and execute actions using various tools and LLM providers.

## 🚀 Features

- **ReAct Pattern Implementation**: Agents use reasoning and acting cycles to solve complex tasks
- **Multiple LLM Providers**: Support for OpenAI, Anthropic, and Ollama
- **Tool Integration**: Extensible tool framework (calculators, web search, file operations)
- **Real-time Streaming**: WebSocket support for streaming agent responses
- **Conversation Management**: Persistent conversation history and state management
- **Token Monitoring**: Comprehensive token usage tracking and limits
- **Security**: JWT and API key authentication with role-based access control
- **Monitoring**: Health checks, metrics, distributed tracing, and alerting
- **Resilience**: Circuit breakers, retries, and graceful degradation
- **Cloud-Native**: Docker and Kubernetes deployment ready

## 📋 Table of Contents

- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Development](#development)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Contributing](#contributing)
- [License](#license)

## 🏃 Quick Start

### Using Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-org/agent-application.git
cd agent-application

# Start all services
docker-compose up -d

# Wait for services to start (about 60 seconds)
# Check application health
curl http://localhost:8080/api/actuator/health

# Access Swagger UI
open http://localhost:8080/api/swagger-ui.html
```

### Local Development

```bash
# Prerequisites: Java 17+, Maven 3.6+, PostgreSQL, Redis

# Build the application
mvn clean package

# Run with development profile
java -jar target/agent-application-*.jar --spring.profiles.active=development
```

## 🏗️ Architecture

The application follows a clean architecture pattern with clear separation of concerns:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Presentation  │    │   Application   │    │  Infrastructure │
│                 │    │                 │    │                 │
│ • REST API      │◄──►│ • Services      │◄──►│ • LLM Providers │
│ • WebSocket     │    │ • Use Cases     │    │ • Tools         │
│ • Controllers   │    │ • Orchestration │    │ • Persistence   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │     Domain      │
                    │                 │
                    │ • Entities      │
                    │ • Value Objects │
                    │ • Interfaces    │
                    └─────────────────┘
```

### Key Components

- **ReAct Engine**: Implements the reasoning and acting pattern
- **LLM Provider Manager**: Manages multiple LLM providers with failover
- **Tool Framework**: Extensible system for agent tools
- **Memory Manager**: Short-term (Redis) and long-term (PostgreSQL) memory
- **Security Layer**: Authentication, authorization, and input validation
- **Monitoring System**: Metrics, health checks, and distributed tracing

## 📦 Prerequisites

### Runtime Requirements

- **Java**: 17 or higher
- **Database**: PostgreSQL 12+ (H2 for development)
- **Cache**: Redis 6+ (optional but recommended)
- **Memory**: Minimum 512MB RAM, recommended 1GB+
- **CPU**: Minimum 1 core, recommended 2+ cores

### Development Requirements

- **Maven**: 3.6 or higher
- **Docker**: 20.10+ (for containerized deployment)
- **kubectl**: 1.20+ (for Kubernetes deployment)

### LLM Provider Requirements (Optional)

- **OpenAI**: API key for GPT models
- **Anthropic**: API key for Claude models
- **Ollama**: Local installation for open-source models

## 🛠️ Installation

### 1. Clone and Build

```bash
git clone https://github.com/your-org/agent-application.git
cd agent-application
mvn clean package
```

### 2. Database Setup

#### PostgreSQL (Production)
```bash
# Create database
createdb agentdb

# Update configuration in application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/agentdb
    username: your_username
    password: your_password
```

#### H2 (Development)
```bash
# No setup required - uses in-memory database
# Access H2 console at: http://localhost:8080/api/h2-console
```

### 3. Redis Setup (Optional)

```bash
# Install Redis
brew install redis  # macOS
sudo apt-get install redis-server  # Ubuntu

# Start Redis
redis-server

# Update configuration
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## ⚙️ Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/agentdb
DB_USERNAME=agent
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# Security
JWT_SECRET=your_jwt_secret_key_256_bits_minimum
JWT_EXPIRATION=86400000

# LLM Providers
OPENAI_API_KEY=your_openai_key
ANTHROPIC_API_KEY=your_anthropic_key
OLLAMA_BASE_URL=http://localhost:11434

# OAuth (Optional)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
```

### Application Profiles

- **development**: Local development with H2 and debug logging
- **docker**: Docker containerized environment
- **production**: Production environment with PostgreSQL and optimized settings

```bash
# Run with specific profile
java -jar app.jar --spring.profiles.active=production
```

## 🎯 Usage

### 1. Authentication

#### Register a new user
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'
```

### 2. Create a Conversation

```bash
curl -X POST http://localhost:8080/api/conversations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "default-agent",
    "title": "Help with coding",
    "initialMessage": "I need help writing a Java function"
  }'
```

### 3. Send Messages

```bash
curl -X POST http://localhost:8080/api/conversations/CONVERSATION_ID/messages \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Can you help me write a fibonacci function?",
    "metadata": {
      "priority": "high"
    }
  }'
```

### 4. Real-time Streaming

```javascript
const ws = new WebSocket('ws://localhost:8080/api/ws/conversations/CONVERSATION_ID');

ws.onmessage = function(event) {
  const message = JSON.parse(event.data);
  console.log('Agent response:', message);
};

ws.send(JSON.stringify({
  type: 'message',
  content: 'Hello, agent!'
}));
```

## 📚 API Documentation

### Interactive Documentation

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api/v3/api-docs
- **Postman Collection**: [docs/Agent-Application-API.postman_collection.json](docs/Agent-Application-API.postman_collection.json)

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new user |
| POST | `/auth/login` | Authenticate user |
| POST | `/conversations` | Create conversation |
| GET | `/conversations/{id}` | Get conversation |
| POST | `/conversations/{id}/messages` | Send message |
| DELETE | `/conversations/{id}` | Terminate conversation |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/metrics` | Application metrics |

For complete API documentation, see [docs/README.md](docs/README.md).

## 🚀 Deployment

### Docker

```bash
# Build image
docker build -t agent-application .

# Run container
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e DATABASE_URL=jdbc:postgresql://host:5432/agentdb \
  agent-application
```

### Docker Compose

```bash
# Development environment
docker-compose up -d

# Production environment
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes

```bash
# Deploy to Kubernetes
kubectl apply -k k8s/

# Deploy to specific environment
kubectl apply -k k8s/overlays/production/
```

For detailed deployment instructions, see:
- [Docker Guide](docker/README.md)
- [Kubernetes Guide](k8s/README.md)

## 💻 Development

### Project Structure

```
src/
├── main/java/com/agent/
│   ├── application/          # Application services
│   ├── domain/              # Domain entities and interfaces
│   ├── infrastructure/      # External integrations
│   └── presentation/        # REST controllers and DTOs
├── main/resources/
│   ├── application.yml      # Configuration
│   └── logback-spring.xml   # Logging configuration
└── test/                    # Test files
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test categories
mvn test -Dtest=*UnitTest
mvn test -Dtest=*IntegrationTest

# Run deployment tests
RUN_DOCKER_TESTS=true mvn test -Dtest=DockerDeploymentTest
```

### Code Quality

```bash
# Run static analysis
mvn spotbugs:check
mvn checkstyle:check

# Generate test coverage report
mvn jacoco:report
```

### Local Development Setup

1. **Start dependencies**:
   ```bash
   docker-compose up -d postgres redis ollama
   ```

2. **Run application**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=development
   ```

3. **Access services**:
   - Application: http://localhost:8080
   - H2 Console: http://localhost:8080/api/h2-console
   - Swagger UI: http://localhost:8080/api/swagger-ui.html

## 🧪 Testing

### Test Categories

- **Unit Tests**: Fast, isolated tests for individual components
- **Integration Tests**: Tests for component interactions
- **Deployment Tests**: Docker and Kubernetes deployment validation
- **API Tests**: Postman collection with automated tests

### Running Tests

```bash
# All tests
mvn test

# Deployment tests
./scripts/test-deployment.sh

# API tests with Newman
newman run docs/Agent-Application-API.postman_collection.json
```

### Test Configuration

Tests use the `test` profile with:
- H2 in-memory database
- Embedded Redis (Testcontainers)
- Mock LLM providers
- Disabled external integrations

## 📊 Monitoring

### Health Checks

- **Basic**: http://localhost:8080/api/actuator/health
- **Detailed**: http://localhost:8080/api/actuator/health/detailed
- **Liveness**: http://localhost:8080/api/actuator/health/liveness
- **Readiness**: http://localhost:8080/api/actuator/health/readiness

### Metrics

- **Prometheus**: http://localhost:8080/api/actuator/prometheus
- **Application Metrics**: http://localhost:8080/api/actuator/metrics
- **Custom Metrics**: Conversation, token usage, and performance metrics

### Distributed Tracing

- **Zipkin**: http://localhost:9411 (when using Docker Compose)
- **Correlation IDs**: Automatic request correlation
- **Performance Monitoring**: Request timing and bottleneck identification

### Dashboards

When using Docker Compose:
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Zipkin**: http://localhost:9411

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and add tests
4. Run tests: `mvn test`
5. Commit your changes: `git commit -m 'Add amazing feature'`
6. Push to the branch: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Code Standards

- Follow Java coding conventions
- Write comprehensive tests
- Update documentation
- Use meaningful commit messages
- Ensure CI/CD pipeline passes

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation

- [API Documentation](docs/README.md)
- [Deployment Guides](docker/README.md)
- [Testing Guide](scripts/README.md)

### Getting Help

- **Issues**: [GitHub Issues](https://github.com/your-org/agent-application/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-org/agent-application/discussions)
- **Email**: support@agent-app.com

### Community

- **Discord**: [Agent App Community](https://discord.gg/agent-app)
- **Twitter**: [@AgentApp](https://twitter.com/AgentApp)

## 🗺️ Roadmap

### Current Version (v1.0.0)
- ✅ ReAct pattern implementation
- ✅ Multiple LLM provider support
- ✅ Tool framework
- ✅ Authentication and authorization
- ✅ Docker and Kubernetes deployment

### Upcoming Features (v1.1.0)
- 🔄 Advanced memory management
- 🔄 Plugin system for custom tools
- 🔄 Multi-agent conversations
- 🔄 Enhanced monitoring and analytics

### Future Plans (v2.0.0)
- 📋 Visual conversation builder
- 📋 Agent marketplace
- 📋 Advanced reasoning capabilities
- 📋 Integration with external systems

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [OpenAI](https://openai.com) - GPT models and API
- [Anthropic](https://anthropic.com) - Claude models
- [Ollama](https://ollama.ai) - Local LLM support
- [ReAct Paper](https://arxiv.org/abs/2210.03629) - Reasoning and Acting pattern

---

**Built with ❤️ by the Agent Application Team**