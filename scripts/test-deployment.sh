#!/bin/bash

# Deployment Testing Script
# This script runs various deployment tests for the Agent Application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_NAMESPACE="agent-application-test"
DOCKER_COMPOSE_FILE="docker-compose.yml"
K8S_MANIFESTS_DIR="k8s"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
    
    # Check if kubectl is available (for K8s tests)
    if ! command -v kubectl &> /dev/null; then
        log_warn "kubectl is not installed - Kubernetes tests will be skipped"
        SKIP_K8S_TESTS=true
    fi
    
    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    log_info "Prerequisites check completed"
}

build_application() {
    log_info "Building application..."
    cd "$PROJECT_ROOT"
    
    # Clean and build the application
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        log_info "Application built successfully"
    else
        log_error "Application build failed"
        exit 1
    fi
}

test_docker_build() {
    log_info "Testing Docker image build..."
    cd "$PROJECT_ROOT"
    
    # Build Docker image
    docker build -t agent-application:test .
    
    if [ $? -eq 0 ]; then
        log_info "Docker image built successfully"
    else
        log_error "Docker image build failed"
        exit 1
    fi
    
    # Test image can start
    log_info "Testing Docker image startup..."
    CONTAINER_ID=$(docker run -d -p 8081:8080 agent-application:test)
    
    # Wait for container to start
    sleep 30
    
    # Check if container is running
    if docker ps | grep -q "$CONTAINER_ID"; then
        log_info "Docker container started successfully"
        
        # Test health endpoint
        if curl -f http://localhost:8081/api/actuator/health > /dev/null 2>&1; then
            log_info "Health endpoint accessible"
        else
            log_warn "Health endpoint not accessible"
        fi
    else
        log_error "Docker container failed to start"
        docker logs "$CONTAINER_ID"
        exit 1
    fi
    
    # Cleanup
    docker stop "$CONTAINER_ID" > /dev/null 2>&1
    docker rm "$CONTAINER_ID" > /dev/null 2>&1
}

test_docker_compose() {
    log_info "Testing Docker Compose deployment..."
    cd "$PROJECT_ROOT"
    
    # Start services
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d
    
    # Wait for services to start
    log_info "Waiting for services to start..."
    sleep 60
    
    # Check if services are running
    if docker-compose -f "$DOCKER_COMPOSE_FILE" ps | grep -q "Up"; then
        log_info "Docker Compose services started successfully"
        
        # Run Docker deployment tests
        log_info "Running Docker deployment tests..."
        RUN_DOCKER_TESTS=true mvn test -Dtest=DockerDeploymentTest
        
        if [ $? -eq 0 ]; then
            log_info "Docker deployment tests passed"
        else
            log_error "Docker deployment tests failed"
            docker-compose -f "$DOCKER_COMPOSE_FILE" logs
            exit 1
        fi
    else
        log_error "Docker Compose services failed to start"
        docker-compose -f "$DOCKER_COMPOSE_FILE" logs
        exit 1
    fi
    
    # Cleanup
    docker-compose -f "$DOCKER_COMPOSE_FILE" down -v
}

test_kubernetes_deployment() {
    if [ "$SKIP_K8S_TESTS" = true ]; then
        log_warn "Skipping Kubernetes tests - kubectl not available"
        return
    fi
    
    log_info "Testing Kubernetes deployment..."
    
    # Check if kubectl is configured
    if ! kubectl cluster-info > /dev/null 2>&1; then
        log_warn "kubectl is not configured - skipping Kubernetes tests"
        return
    fi
    
    # Create test namespace
    kubectl create namespace "$TEST_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    
    # Deploy application to test namespace
    log_info "Deploying to Kubernetes test namespace..."
    kubectl apply -k "$K8S_MANIFESTS_DIR/overlays/development" -n "$TEST_NAMESPACE"
    
    # Wait for deployment
    log_info "Waiting for deployment to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/agent-app -n "$TEST_NAMESPACE"
    
    if [ $? -eq 0 ]; then
        log_info "Kubernetes deployment successful"
        
        # Run Kubernetes deployment tests
        log_info "Running Kubernetes deployment tests..."
        RUN_K8S_TESTS=true mvn test -Dtest=KubernetesDeploymentTest
        
        if [ $? -eq 0 ]; then
            log_info "Kubernetes deployment tests passed"
        else
            log_error "Kubernetes deployment tests failed"
            kubectl describe deployment agent-app -n "$TEST_NAMESPACE"
            kubectl logs -l app.kubernetes.io/name=agent-application -n "$TEST_NAMESPACE"
            exit 1
        fi
    else
        log_error "Kubernetes deployment failed"
        kubectl describe deployment agent-app -n "$TEST_NAMESPACE"
        exit 1
    fi
    
    # Cleanup
    kubectl delete namespace "$TEST_NAMESPACE" --ignore-not-found=true
}

test_api_documentation() {
    log_info "Testing API documentation..."
    cd "$PROJECT_ROOT"
    
    # Run API documentation tests
    mvn test -Dtest=ApiDocumentationTest
    
    if [ $? -eq 0 ]; then
        log_info "API documentation tests passed"
    else
        log_error "API documentation tests failed"
        exit 1
    fi
    
    # Validate OpenAPI specification
    if command -v swagger-codegen &> /dev/null; then
        log_info "Validating OpenAPI specification..."
        swagger-codegen validate -i docs/openapi.yaml
        
        if [ $? -eq 0 ]; then
            log_info "OpenAPI specification is valid"
        else
            log_warn "OpenAPI specification validation failed"
        fi
    else
        log_warn "swagger-codegen not available - skipping OpenAPI validation"
    fi
}

run_performance_tests() {
    log_info "Running basic performance tests..."
    
    # Start application for performance testing
    cd "$PROJECT_ROOT"
    docker-compose -f "$DOCKER_COMPOSE_FILE" up -d agent-app postgres redis
    
    # Wait for application to start
    sleep 60
    
    # Run basic load test with curl
    log_info "Running load test on health endpoint..."
    for i in {1..100}; do
        curl -s http://localhost:8080/api/actuator/health > /dev/null
        if [ $((i % 10)) -eq 0 ]; then
            echo -n "."
        fi
    done
    echo ""
    
    # Check if application is still responsive
    if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        log_info "Application remained responsive under load"
    else
        log_warn "Application may have issues under load"
    fi
    
    # Cleanup
    docker-compose -f "$DOCKER_COMPOSE_FILE" down
}

cleanup() {
    log_info "Cleaning up..."
    
    # Stop any running Docker containers
    docker-compose -f "$DOCKER_COMPOSE_FILE" down -v > /dev/null 2>&1 || true
    
    # Remove test images
    docker rmi agent-application:test > /dev/null 2>&1 || true
    
    # Clean up Kubernetes test namespace
    if [ "$SKIP_K8S_TESTS" != true ]; then
        kubectl delete namespace "$TEST_NAMESPACE" --ignore-not-found=true > /dev/null 2>&1 || true
    fi
    
    log_info "Cleanup completed"
}

# Main execution
main() {
    log_info "Starting deployment tests..."
    
    # Set up cleanup trap
    trap cleanup EXIT
    
    # Run tests
    check_prerequisites
    build_application
    test_docker_build
    test_docker_compose
    test_kubernetes_deployment
    test_api_documentation
    run_performance_tests
    
    log_info "All deployment tests completed successfully!"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-k8s)
            SKIP_K8S_TESTS=true
            shift
            ;;
        --skip-docker)
            SKIP_DOCKER_TESTS=true
            shift
            ;;
        --skip-perf)
            SKIP_PERF_TESTS=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --skip-k8s     Skip Kubernetes tests"
            echo "  --skip-docker  Skip Docker tests"
            echo "  --skip-perf    Skip performance tests"
            echo "  -h, --help     Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Run main function
main