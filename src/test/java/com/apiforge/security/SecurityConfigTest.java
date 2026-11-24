package com.apiforge.security;

import com.apiforge.user.UserRole;
import com.apiforge.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void jwtCanBeSignedAndValidatedWithConfiguredIssuer() {
        SecretKey key = securityConfig.jwtSecretKey(
                "unit-test-secret-that-is-at-least-32-bytes-long");
        var encoder = securityConfig.jwtEncoder(key);
        var decoder = securityConfig.jwtDecoder(key);
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("apiforge")
                .subject("8d3c2d2a-51a9-4d3f-8e9c-0fce15a6b888")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("USER"))
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        Jwt decoded = decoder.decode(token);
        assertEquals("apiforge", decoded.getClaimAsString("iss"));
        assertEquals(claims.getSubject(), decoded.getSubject());
        assertEquals(List.of("USER"), decoded.getClaimAsStringList("roles"));
    }

    @Test
    void jwtSecretShorterThanThirtyTwoBytesIsRejected() {
        assertThrows(IllegalStateException.class, () -> securityConfig.jwtSecretKey("too-short"));
    }

    @Test
    void roleClaimMapsToSpringRoleAuthority() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("roles", List.of("ADMIN"))
                .build();

        var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void suspendedUserIsNotEnabled() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "suspended@example.com",
                "Suspended User", "hash", UserRole.USER, UserStatus.SUSPENDED);
        assertFalse(principal.isEnabled());
        assertEquals("suspended@example.com", principal.getUsername());
    }
}