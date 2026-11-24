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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationIntegrationTest {

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
        registry.add("management.health.redis.enabled", () -> "false");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @Test
    void registrationLoginAndProtectedProfileWorkAgainstPostgres() {
        String email = uniqueEmail();
        String password = "Integration!Test2026";

        ResponseEntity<JsonNode> registration = post("/api/v1/auth/register",
                Map.of("email", email, "displayName", "Integration User", "password", password), null);

        assertEquals(HttpStatus.CREATED, registration.getStatusCode());
        String accessToken = registration.getBody().get("accessToken").asText();
        assertFalse(accessToken.isBlank());

        ResponseEntity<JsonNode> profile = get("/api/v1/users/me", accessToken);
        assertEquals(HttpStatus.OK, profile.getStatusCode());
        assertEquals(email, profile.getBody().get("email").asText());

        ResponseEntity<JsonNode> anonymousProfile = get("/api/v1/users/me", null);
        assertEquals(HttpStatus.UNAUTHORIZED, anonymousProfile.getStatusCode());

        ResponseEntity<JsonNode> login = post("/api/v1/auth/login",
                Map.of("email", email, "password", password), null);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        assertFalse(login.getBody().get("accessToken").asText().isBlank());

        ResponseEntity<JsonNode> invalidLogin = post("/api/v1/auth/login",
                Map.of("email", email, "password", "Wrong!Password2026"), null);
        assertEquals(HttpStatus.UNAUTHORIZED, invalidLogin.getStatusCode());

        ResponseEntity<JsonNode> duplicate = post("/api/v1/auth/register",
                Map.of("email", email.toUpperCase(), "displayName", "Duplicate User",
                        "password", "Another!Password2026"), null);
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM platform_users WHERE email = ?",
                String.class, email);
        assertTrue(passwordHash.startsWith("$2"),
                "The database must contain a BCrypt hash, not the submitted password");
        assertNotEquals(password, passwordHash);
    }

    @Test
    void apiKeyLifecycleIsPersistedAndIsolatedByApplicationOwner() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "Owner User");
        String otherUserToken = registerAndGetToken(uniqueEmail(), "Other User");

        ResponseEntity<JsonNode> applicationResponse = post("/api/v1/applications",
                Map.of("name", "Integration Application",
                        "description", "Created by the integration test"), ownerToken);
        assertEquals(HttpStatus.CREATED, applicationResponse.getStatusCode());
        String applicationId = applicationResponse.getBody().get("id").asText();

        ResponseEntity<JsonNode> createdKey = post(
                "/api/v1/applications/" + applicationId + "/keys",
                Map.of("name", "Integration Key"), ownerToken);
        assertEquals(HttpStatus.CREATED, createdKey.getStatusCode());

        String keyId = createdKey.getBody().get("id").asText();
        String plaintextKey = createdKey.getBody().get("apiKey").asText();
        assertTrue(plaintextKey.startsWith("af_live_"));

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT key_hash FROM api_keys WHERE id = ?",
                String.class, UUID.fromString(keyId));
        assertNotEquals(plaintextKey, storedHash);
        assertTrue(storedHash.matches("[0-9a-f]{64}"));

        ResponseEntity<JsonNode> keyList = get(
                "/api/v1/applications/" + applicationId + "/keys", ownerToken);
        assertEquals(HttpStatus.OK, keyList.getStatusCode());
        JsonNode listedKey = keyList.getBody().get(0);
        assertEquals("ACTIVE", listedKey.get("status").asText());
        assertFalse(listedKey.has("apiKey"),
                "The list endpoint must never return the plaintext API key");

        ResponseEntity<JsonNode> otherUsersKeyList = get(
                "/api/v1/applications/" + applicationId + "/keys", otherUserToken);
        assertEquals(HttpStatus.NOT_FOUND, otherUsersKeyList.getStatusCode());

        ResponseEntity<JsonNode> revoke = exchange(
                "/api/v1/applications/" + applicationId + "/keys/" + keyId,
                HttpMethod.DELETE, null, ownerToken);
        assertEquals(HttpStatus.NO_CONTENT, revoke.getStatusCode());

        ResponseEntity<JsonNode> afterRevoke = get(
                "/api/v1/applications/" + applicationId + "/keys", ownerToken);
        assertEquals("REVOKED", afterRevoke.getBody().get(0).get("status").asText());
    }

    private String registerAndGetToken(String email, String displayName) {
        ResponseEntity<JsonNode> response = post("/api/v1/auth/register",
                Map.of("email", email, "displayName", displayName,
                        "password", "Integration!Test2026"), null);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody().get("accessToken").asText();
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
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange("http://localhost:" + port + path, method,
                new HttpEntity<>(body, headers), JsonNode.class);
    }

    private String uniqueEmail() {
        return "integration-" + UUID.randomUUID() + "@example.com";
    }
}
