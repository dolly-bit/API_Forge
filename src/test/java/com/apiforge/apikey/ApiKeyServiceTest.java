package com.apiforge.apikey;

import com.apiforge.application.PlatformApplication;
import com.apiforge.application.PlatformApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeys;

    @Mock
    private PlatformApplicationRepository applications;

    @Test
    void createReturnsPlaintextOnceAndPersistsOnlySha256Hash() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        when(applications.findByIdAndOwnerUserId(applicationId, ownerId))
                .thenReturn(Optional.of(new PlatformApplication(ownerId, "sample-app", null)));
        when(apiKeys.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyService service = new ApiKeyService(apiKeys, applications);
        ApiKeyService.CreatedApiKey created = service.create(ownerId, applicationId, "Production", null);

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeys).save(captor.capture());
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(created.apiKey().getBytes(StandardCharsets.UTF_8)));

        assertTrue(created.apiKey().startsWith("af_live_"));
        assertEquals(expectedHash, captor.getValue().getKeyHash());
        assertNotEquals(created.apiKey(), captor.getValue().getKeyHash());
        assertEquals(created.apiKey().substring(0, 16), created.keyPrefix());
    }

    @Test
    void userCannotCreateKeyForAnotherUsersApplication() {
        UUID ownerId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        when(applications.findByIdAndOwnerUserId(applicationId, ownerId)).thenReturn(Optional.empty());

        ApiKeyService service = new ApiKeyService(apiKeys, applications);
        assertThrows(ResponseStatusException.class,
                () -> service.create(ownerId, applicationId, "Production", null));
        verify(apiKeys, never()).save(any(ApiKey.class));
    }

    @Test
    void validatorHashesPresentedKeyBeforeDatabaseLookup() throws Exception {
        String rawKey = "af_live_test-client-key";
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        ApiKey storedKey = new ApiKey(UUID.randomUUID(), "Production", "af_live_test", expectedHash, null);
        when(apiKeys.findByKeyHashAndStatus(expectedHash, ApiKeyStatus.ACTIVE))
                .thenReturn(Optional.of(storedKey));

        ApiKeyService service = new ApiKeyService(apiKeys, applications);
        assertTrue(service.isValid(rawKey));
        assertFalse(service.isValid("invalid-prefix"));
        verify(apiKeys).findByKeyHashAndStatus(expectedHash, ApiKeyStatus.ACTIVE);
        verify(apiKeys, never()).findByKeyHashAndStatus(eq(rawKey), any(ApiKeyStatus.class));
    }
}