package com.apiforge.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoutingManagementIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret",
                () -> "integration-test-secret-with-at-least-32-bytes");
        registry.add("apiforge.gateway.allowed-hosts", () -> "api.example.com");
        registry.add("management.health.redis.enabled", () -> "false");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate rest;

    @LocalServerPort
    private int port;

    @Test
    void ownerCanManageAllowlistedUpstreamsAndMethodAwareRoutes() {
        String ownerToken = registerAndGetToken("owner");
        String otherToken = registerAndGetToken("other");

        ResponseEntity<JsonNode> application = post("/api/v1/applications",
                Map.of("name", "Gateway App"), ownerToken);
        assertEquals(HttpStatus.CREATED, application.getStatusCode());
        String applicationId = application.getBody().get("id").asText();

        ResponseEntity<JsonNode> upstream = post(
                "/api/v1/applications/" + applicationId + "/upstreams",
                Map.of("name", "Orders API", "baseUrl", "https://api.example.com/v1"), ownerToken);
        assertEquals(HttpStatus.CREATED, upstream.getStatusCode());
        String upstreamId = upstream.getBody().get("id").asText();
        assertEquals("https://api.example.com/v1", upstream.getBody().get("baseUrl").asText());

        ResponseEntity<JsonNode> route = post(
                routesPath(applicationId, upstreamId),
                Map.of("routeKey", "Orders", "httpMethod", "GET",
                        "upstreamPathPrefix", "/orders"), ownerToken);
        assertEquals(HttpStatus.CREATED, route.getStatusCode());
        assertEquals("orders", route.getBody().get("routeKey").asText());
        assertEquals("GET", route.getBody().get("httpMethod").asText());
        assertEquals("/orders", route.getBody().get("upstreamPathPrefix").asText());

        ResponseEntity<JsonNode> duplicate = post(
                routesPath(applicationId, upstreamId),
                Map.of("routeKey", "orders", "httpMethod", "GET"), ownerToken);
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());

        ResponseEntity<JsonNode> otherOwner = get(
                "/api/v1/applications/" + applicationId + "/upstreams", otherToken);
        assertEquals(HttpStatus.NOT_FOUND, otherOwner.getStatusCode());

        ResponseEntity<JsonNode> disallowedHost = post(
                "/api/v1/applications/" + applicationId + "/upstreams",
                Map.of("name", "Untrusted API", "baseUrl", "https://not-allowed.example.net"), ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, disallowedHost.getStatusCode());

        ResponseEntity<JsonNode> insecureUrl = post(
                "/api/v1/applications/" + applicationId + "/upstreams",
                Map.of("name", "Insecure API", "baseUrl", "http://api.example.com"), ownerToken);
        assertEquals(HttpStatus.BAD_REQUEST, insecureUrl.getStatusCode());

        ResponseEntity<JsonNode> disabled = exchange(
                "/api/v1/applications/" + applicationId + "/upstreams/" + upstreamId + "/status",
                HttpMethod.PATCH, Map.of("status", "DISABLED"), ownerToken);
        assertEquals(HttpStatus.OK, disabled.getStatusCode());
        assertEquals("DISABLED", disabled.getBody().get("status").asText());

        ResponseEntity<JsonNode> routeOnDisabledUpstream = post(
                routesPath(applicationId, upstreamId),
                Map.of("routeKey", "billing", "httpMethod", "POST"), ownerToken);
        assertEquals(HttpStatus.CONFLICT, routeOnDisabledUpstream.getStatusCode());

        ResponseEntity<JsonNode> anonymous = get(
                "/api/v1/applications/" + applicationId + "/upstreams", null);
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getStatusCode());
        assertTrue(route.getBody().has("id"));
        assertFalse(route.getBody().has("apiKey"));
    }

    private String registerAndGetToken(String label) {
        ResponseEntity<JsonNode> response = post("/api/v1/auth/register",
                Map.of("email", label + "-" + UUID.randomUUID() + "@example.com",
                        "displayName", label, "password", "Integration!Test2026"), null);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody().get("accessToken").asText();
    }

    private String routesPath(String applicationId, String upstreamId) {
        return "/api/v1/applications/" + applicationId + "/upstreams/"
                + upstreamId + "/routes";
    }

    private ResponseEntity<JsonNode> post(String path, Object body, String token) {
        return exchange(path, HttpMethod.POST, body, token);
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token);
    }

    private ResponseEntity<JsonNode> exchange(
            String path, HttpMethod method, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return rest.exchange("http://localhost:" + port + path, method,
                new HttpEntity<>(body, headers), JsonNode.class);
    }
}
