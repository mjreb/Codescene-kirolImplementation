# Agent Application API Documentation

This directory contains comprehensive API documentation for the Agent Application.

## Documentation Files

### OpenAPI Specification
- **[openapi.yaml](openapi.yaml)**: Complete OpenAPI 3.0 specification
- **Swagger UI**: Available at `http://localhost:8080/api/swagger-ui.html` when the application is running
- **API Docs**: Available at `http://localhost:8080/api/v3/api-docs` (JSON format)

### Postman Collection
- **[Agent-Application-API.postman_collection.json](Agent-Application-API.postman_collection.json)**: Complete Postman collection with examples and tests

## Quick Start

### 1. View Interactive Documentation

Start the application and visit:
```
http://localhost:8080/api/swagger-ui.html
```

This provides an interactive interface where you can:
- Browse all API endpoints
- View request/response schemas
- Test endpoints directly from the browser
- Download the OpenAPI specification

### 2. Import Postman Collection

1. Open Postman
2. Click "Import" 
3. Select the `Agent-Application-API.postman_collection.json` file
4. The collection will be imported with all endpoints and examples

### 3. Set Up Environment Variables

In Postman, create environment variables:
- `base_url`: `http://localhost:8080/api`
- `access_token`: (will be set automatically after login)
- `refresh_token`: (will be set automatically after login)
- `conversation_id`: (will be set automatically after creating a conversation)
- `api_key`: (will be set automatically after generating an API key)

## API Overview

### Base URL
- **Development**: `http://localhost:8080/api`
- **Production**: `https://api.agent-app.com/api`

### Authentication

The API supports two authentication methods:

#### 1. JWT Bearer Token
```bash
# Login to get token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'

# Use token in subsequent requests
curl -X GET http://localhost:8080/api/conversations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 2. API Key
```bash
# Generate API key (requires authentication)
curl -X POST http://localhost:8080/api/auth/api-key?name=my-key \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Use API key in requests
curl -X GET http://localhost:8080/api/conversations \
  -H "X-API-Key: YOUR_API_KEY"
```

### Core Endpoints

#### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Authenticate user
- `POST /auth/refresh` - Refresh JWT token
- `POST /auth/api-key` - Generate API key

#### Conversations
- `POST /conversations` - Create new conversation
- `GET /conversations/{id}` - Get conversation details
- `POST /conversations/{id}/messages` - Send message
- `DELETE /conversations/{id}` - Terminate conversation

#### Monitoring
- `GET /actuator/health` - System health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Example Usage

### 1. Complete Conversation Flow

```bash
# 1. Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123",
    "firstName": "John",
    "lastName": "Doe"
  }'

# 2. Create conversation
curl -X POST http://localhost:8080/api/conversations \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "default-agent",
    "title": "Help with coding",
    "initialMessage": "I need help with Java programming"
  }'

# 3. Send message
curl -X POST http://localhost:8080/api/conversations/CONVERSATION_ID/messages \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Can you help me write a fibonacci function?",
    "metadata": {
      "priority": "high"
    }
  }'

# 4. Get conversation history
curl -X GET http://localhost:8080/api/conversations/CONVERSATION_ID \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 2. Health Check

```bash
# Basic health check
curl -X GET http://localhost:8080/api/actuator/health

# Detailed health check (requires authentication)
curl -X GET http://localhost:8080/api/actuator/health/detailed \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Response Formats

### Success Response
```json
{
  "id": "conv-123",
  "agentId": "default-agent",
  "title": "Help with coding",
  "status": "ACTIVE",
  "createdAt": "2024-01-01T10:00:00Z",
  "messageCount": 2,
  "tokenUsage": {
    "totalTokens": 150,
    "inputTokens": 50,
    "outputTokens": 100
  }
}
```

### Error Response
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "details": [
    "Field 'content' is required"
  ],
  "timestamp": "2024-01-01T10:00:00Z",
  "path": "/conversations/123/messages"
}
```

## Rate Limits

- **Authenticated Users**: 100 requests per minute
- **API Keys**: 1000 requests per hour
- **Token Limits**: Configurable per user and conversation

## Error Codes

| Code | Description |
|------|-------------|
| 400 | Bad Request - Invalid request data |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource conflict |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error - Server error |

## WebSocket Support

For real-time streaming, the API supports WebSocket connections:

```javascript
const ws = new WebSocket('ws://localhost:8080/api/ws/conversations/CONVERSATION_ID');

ws.onmessage = function(event) {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};

ws.send(JSON.stringify({
  type: 'message',
  content: 'Hello, agent!'
}));
```

## SDK and Client Libraries

### JavaScript/TypeScript
```bash
npm install @agent-app/client
```

```javascript
import { AgentClient } from '@agent-app/client';

const client = new AgentClient({
  baseUrl: 'http://localhost:8080/api',
  apiKey: 'your-api-key'
});

const conversation = await client.conversations.create({
  agentId: 'default-agent',
  title: 'My Conversation'
});

const response = await client.conversations.sendMessage(conversation.id, {
  content: 'Hello, agent!'
});
```

### Python
```bash
pip install agent-app-client
```

```python
from agent_app import AgentClient

client = AgentClient(
    base_url='http://localhost:8080/api',
    api_key='your-api-key'
)

conversation = client.conversations.create(
    agent_id='default-agent',
    title='My Conversation'
)

response = client.conversations.send_message(
    conversation.id,
    content='Hello, agent!'
)
```

## Testing

### Unit Tests
The Postman collection includes automated tests for all endpoints:
- Authentication flow
- Conversation management
- Error handling
- Rate limiting

### Load Testing
Example using Apache Bench:
```bash
# Test conversation creation
ab -n 100 -c 10 -H "Authorization: Bearer YOUR_TOKEN" \
   -p conversation.json -T application/json \
   http://localhost:8080/api/conversations
```

## Monitoring and Observability

### Metrics
- Request/response times
- Error rates
- Token usage
- Conversation statistics

### Health Checks
- Database connectivity
- Redis connectivity
- LLM provider status
- Memory usage

### Distributed Tracing
- Zipkin integration
- Request correlation IDs
- Performance monitoring

## Security

### Best Practices
- Use HTTPS in production
- Rotate API keys regularly
- Implement proper CORS policies
- Monitor for suspicious activity
- Use strong JWT secrets

### OWASP Compliance
- Input validation
- SQL injection prevention
- XSS protection
- CSRF protection
- Rate limiting

## Support

For API support and questions:
- **Documentation**: This README and OpenAPI spec
- **Issues**: GitHub Issues
- **Email**: support@agent-app.com
- **Discord**: [Agent App Community](https://discord.gg/agent-app)

## Changelog

### v1.0.0 (2024-01-01)
- Initial API release
- JWT and API key authentication
- Conversation management
- ReAct agent integration
- Health monitoring
- OpenAPI documentation