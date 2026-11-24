package com.apiforge.apikey;

import com.apiforge.application.PlatformApplication;
import com.apiforge.application.PlatformApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "af_live_";
    private final SecureRandom secureRandom = new SecureRandom();
    private final ApiKeyRepository apiKeys;
    private final PlatformApplicationRepository applications;

    public ApiKeyService(ApiKeyRepository apiKeys, PlatformApplicationRepository applications) {
        this.apiKeys = apiKeys;
        this.applications = applications;
    }

    @Transactional
    public CreatedApiKey create(UUID ownerId, UUID applicationId, String name, Instant expiresAt) {
        PlatformApplication application = applications.findByIdAndOwnerUserId(applicationId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (!"ACTIVE".equals(application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not active");
        }
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiration must be in the future");
        }

        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String rawKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String hash = sha256(rawKey);
        String prefix = rawKey.substring(0, 16);

        ApiKey saved = apiKeys.save(new ApiKey(applicationId, name.trim(), prefix, hash, expiresAt));
        return new CreatedApiKey(saved.getId(), saved.getName(), saved.getKeyPrefix(),
                rawKey, saved.getExpiresAt(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ApiKeySummary> list(UUID ownerId, UUID applicationId) {
        requireOwnedApplication(ownerId, applicationId);
        return apiKeys.findAllByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream().map(ApiKeySummary::from).toList();
    }

    @Transactional
    public void revoke(UUID ownerId, UUID applicationId, UUID keyId) {
        requireOwnedApplication(ownerId, applicationId);
        ApiKey key = apiKeys.findByIdAndApplicationId(keyId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        if (key.getStatus() == ApiKeyStatus.REVOKED) return;
        key.revoke(Instant.now());
    }

    @Transactional
    public boolean isValid(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) return false;
        return apiKeys.findByKeyHashAndStatus(sha256(rawKey), ApiKeyStatus.ACTIVE)
                .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(Instant.now()))
                .map(key -> {
                    key.markUsed(Instant.now());
                    return true;
                }).orElse(false);
    }

    private void requireOwnedApplication(UUID ownerId, UUID applicationId) {
        applications.findByIdAndOwnerUserId(applicationId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record CreatedApiKey(UUID id, String name, String keyPrefix, String apiKey,
                                Instant expiresAt, Instant createdAt) { }

    public record ApiKeySummary(UUID id, String name, String keyPrefix, String status,
                                Instant expiresAt, Instant lastUsedAt, Instant createdAt) {
        static ApiKeySummary from(ApiKey key) {
            return new ApiKeySummary(key.getId(), key.getName(), key.getKeyPrefix(),
                    key.getStatus().name(), key.getExpiresAt(), key.getLastUsedAt(), key.getCreatedAt());
        }
    }
}