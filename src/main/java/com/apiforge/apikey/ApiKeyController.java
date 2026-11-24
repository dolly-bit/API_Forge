package com.apiforge.apikey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyService.CreatedApiKey create(
            @PathVariable UUID applicationId,
            @Valid @RequestBody CreateApiKeyRequest request,
            Jwt jwt) {
        return apiKeyService.create(UUID.fromString(jwt.getSubject()), applicationId,
                request.name(), request.expiresAt());
    }

    @GetMapping
    public List<ApiKeyService.ApiKeySummary> list(@PathVariable UUID applicationId, Jwt jwt) {
        return apiKeyService.list(UUID.fromString(jwt.getSubject()), applicationId);
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID applicationId, @PathVariable UUID keyId, Jwt jwt) {
        apiKeyService.revoke(UUID.fromString(jwt.getSubject()), applicationId, keyId);
    }

    public record CreateApiKeyRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            Instant expiresAt
    ) { }
}