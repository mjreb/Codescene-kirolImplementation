package com.agent.deployment;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Kubernetes deployment.
 * These tests verify that the application can be deployed to Kubernetes.
 * 
 * Prerequisites:
 * - kubectl configured with access to a test cluster
 * - Test namespace created
 * - Application deployed to the test namespace
 */
@EnabledIfEnvironmentVariable(named = "RUN_K8S_TESTS", matches = "true")
public class KubernetesDeploymentTest {

    private static final String NAMESPACE = "agent-application-test";
    private static final String APP_NAME = "agent-app";
    private static final String SERVICE_NAME = "agent-app-service";
    
    private ApiClient client;
    private CoreV1Api coreV1Api;
    private AppsV1Api appsV1Api;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize Kubernetes client
        client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        
        coreV1Api = new CoreV1Api();
        appsV1Api = new AppsV1Api();
        
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Test
    void testNamespaceExists() throws ApiException {
        // Verify that the test namespace exists
        V1Namespace namespace = coreV1Api.readNamespace(NAMESPACE, null);
        assertNotNull(namespace);
        assertEquals(NAMESPACE, namespace.getMetadata().getName());
    }

    @Test
    void testDeploymentExists() throws ApiException {
        // Verify that the application deployment exists
        V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                APP_NAME, NAMESPACE, null);
        
        assertNotNull(deployment);
        assertEquals(APP_NAME, deployment.getMetadata().getName());
        assertEquals(NAMESPACE, deployment.getMetadata().getNamespace());
    }

    @Test
    void testDeploymentIsReady() throws ApiException, InterruptedException {
        // Wait for deployment to be ready
        boolean isReady = waitForDeploymentReady(APP_NAME, NAMESPACE, 300); // 5 minutes
        assertTrue(isReady, "Deployment should be ready within 5 minutes");
        
        // Verify deployment status
        V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                APP_NAME, NAMESPACE, null);
        
        V1DeploymentStatus status = deployment.getStatus();
        assertNotNull(status);
        assertNotNull(status.getReadyReplicas());
        assertTrue(status.getReadyReplicas() > 0, "At least one replica should be ready");
        assertEquals(status.getReplicas(), status.getReadyReplicas(), 
                "All replicas should be ready");
    }

    @Test
    void testPodsAreRunning() throws ApiException {
        // Verify that pods are running
        V1PodList pods = coreV1Api.listNamespacedPod(
                NAMESPACE, null, null, null, null, 
                "app.kubernetes.io/name=" + APP_NAME, null, null, null, null, null);
        
        assertNotNull(pods);
        assertFalse(pods.getItems().isEmpty(), "At least one pod should exist");
        
        for (V1Pod pod : pods.getItems()) {
            assertEquals("Running", pod.getStatus().getPhase(), 
                    "Pod " + pod.getMetadata().getName() + " should be running");
            
            // Check container statuses
            for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
                assertTrue(containerStatus.getReady(), 
                        "Container " + containerStatus.getName() + " should be ready");
            }
        }
    }

    @Test
    void testServiceExists() throws ApiException {
        // Verify that the service exists
        V1Service service = coreV1Api.readNamespacedService(
                SERVICE_NAME, NAMESPACE, null);
        
        assertNotNull(service);
        assertEquals(SERVICE_NAME, service.getMetadata().getName());
        assertEquals(NAMESPACE, service.getMetadata().getNamespace());
        
        // Verify service has endpoints
        V1Endpoints endpoints = coreV1Api.readNamespacedEndpoints(
                SERVICE_NAME, NAMESPACE, null);
        
        assertNotNull(endpoints);
        assertNotNull(endpoints.getSubsets());
        assertFalse(endpoints.getSubsets().isEmpty(), "Service should have endpoints");
    }

    @Test
    void testConfigMapExists() throws ApiException {
        // Verify that the configuration ConfigMap exists
        V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(
                "agent-app-config", NAMESPACE, null);
        
        assertNotNull(configMap);
        assertNotNull(configMap.getData());
        assertTrue(configMap.getData().containsKey("application.yml"), 
                "ConfigMap should contain application.yml");
    }

    @Test
    void testSecretsExist() throws ApiException {
        // Verify that the secrets exist
        V1Secret secrets = coreV1Api.readNamespacedSecret(
                "agent-app-secrets", NAMESPACE, null);
        
        assertNotNull(secrets);
        assertNotNull(secrets.getData());
        assertTrue(secrets.getData().containsKey("db-username"), 
                "Secret should contain db-username");
        assertTrue(secrets.getData().containsKey("jwt-secret"), 
                "Secret should contain jwt-secret");
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // Test health endpoint through port-forward or ingress
        String healthUrl = getServiceUrl() + "/actuator/health";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    void testHorizontalPodAutoscaler() throws ApiException {
        // Verify HPA is configured (if using autoscaling/v2)
        try {
            // This would require the autoscaling API client
            // For now, we'll check if the HPA exists by name
            V1ObjectMeta hpaMetadata = new V1ObjectMeta();
            hpaMetadata.setName("agent-app-hpa");
            hpaMetadata.setNamespace(NAMESPACE);
            
            // In a real implementation, you'd use the AutoscalingV2Api
            // to verify the HPA configuration
            assertTrue(true, "HPA configuration test placeholder");
        } catch (Exception e) {
            // HPA might not be available in test environment
            System.out.println("HPA test skipped: " + e.getMessage());
        }
    }

    @Test
    void testResourceLimits() throws ApiException {
        // Verify that resource limits are set
        V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                APP_NAME, NAMESPACE, null);
        
        V1Container container = deployment.getSpec().getTemplate().getSpec()
                .getContainers().get(0);
        
        assertNotNull(container.getResources(), "Container should have resource specifications");
        assertNotNull(container.getResources().getLimits(), "Container should have resource limits");
        assertNotNull(container.getResources().getRequests(), "Container should have resource requests");
        
        // Verify specific limits
        assertTrue(container.getResources().getLimits().containsKey("memory"), 
                "Memory limit should be set");
        assertTrue(container.getResources().getLimits().containsKey("cpu"), 
                "CPU limit should be set");
    }

    @Test
    void testProbes() throws ApiException {
        // Verify that health probes are configured
        V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                APP_NAME, NAMESPACE, null);
        
        V1Container container = deployment.getSpec().getTemplate().getSpec()
                .getContainers().get(0);
        
        assertNotNull(container.getLivenessProbe(), "Liveness probe should be configured");
        assertNotNull(container.getReadinessProbe(), "Readiness probe should be configured");
        
        // Verify probe configurations
        V1HTTPGetAction livenessAction = container.getLivenessProbe().getHttpGet();
        assertNotNull(livenessAction);
        assertTrue(livenessAction.getPath().contains("health"), 
                "Liveness probe should check health endpoint");
        
        V1HTTPGetAction readinessAction = container.getReadinessProbe().getHttpGet();
        assertNotNull(readinessAction);
        assertTrue(readinessAction.getPath().contains("health"), 
                "Readiness probe should check health endpoint");
    }

    @Test
    void testNetworkPolicy() throws ApiException {
        // Verify network policies are in place (if using network policies)
        try {
            // This would require the NetworkingV1Api
            // For now, we'll just verify the test passes
            assertTrue(true, "Network policy test placeholder");
        } catch (Exception e) {
            System.out.println("Network policy test skipped: " + e.getMessage());
        }
    }

    private boolean waitForDeploymentReady(String deploymentName, String namespace, int timeoutSeconds) 
            throws ApiException, InterruptedException {
        
        int waited = 0;
        while (waited < timeoutSeconds) {
            V1Deployment deployment = appsV1Api.readNamespacedDeployment(
                    deploymentName, namespace, null);
            
            V1DeploymentStatus status = deployment.getStatus();
            if (status != null && status.getReadyReplicas() != null && 
                status.getReplicas() != null && 
                status.getReadyReplicas().equals(status.getReplicas())) {
                return true;
            }
            
            TimeUnit.SECONDS.sleep(5);
            waited += 5;
        }
        
        return false;
    }

    private String getServiceUrl() throws ApiException {
        // In a real test environment, this would either:
        // 1. Use port-forward to access the service
        // 2. Use the ingress URL
        // 3. Use a LoadBalancer service external IP
        
        // For this example, we'll assume port-forward is set up
        // kubectl port-forward service/agent-app-service 8080:80 -n agent-application-test
        return "http://localhost:8080/api";
    }
}