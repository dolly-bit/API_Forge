package com.apiforge.upstream;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpstreamUrlPolicyTest {

    private final UpstreamUrlPolicy policy = new UpstreamUrlPolicy("api.example.com,files.example.com");

    @Test
    void acceptsOnlyExactAllowlistedHttpsHosts() {
        assertEquals("https://api.example.com/v1",
                policy.validateAndNormalize("https://api.example.com/v1"));
        assertEquals("https://files.example.com",
                policy.validateAndNormalize("https://files.example.com/"));
    }

    @Test
    void rejectsUntrustedSchemesHostsAndPrivateDestinations() {
        assertThrows(ResponseStatusException.class,
                () -> policy.validateAndNormalize("http://api.example.com"));
        assertThrows(ResponseStatusException.class,
                () -> policy.validateAndNormalize("https://evil.example.com"));
        assertThrows(ResponseStatusException.class,
                () -> policy.validateAndNormalize("https://127.0.0.1"));
        assertThrows(ResponseStatusException.class,
                () -> policy.validateAndNormalize("https://localhost"));
        assertThrows(ResponseStatusException.class,
                () -> policy.validateAndNormalize("https://user:secret@api.example.com"));
        assertThrows(ResponseStatusException.class,
                () -> policy.validateAndNormalize("https://api.example.com?token=secret"));
    }

    @Test
    void emptyAllowlistRejectsEveryDestination() {
        UpstreamUrlPolicy emptyPolicy = new UpstreamUrlPolicy("");
        assertThrows(ResponseStatusException.class,
                () -> emptyPolicy.validateAndNormalize("https://api.example.com"));
    }
}
