package com.apiforge.auth;

import com.apiforge.auth.dto.AuthResponse;
import com.apiforge.auth.dto.LoginRequest;
import com.apiforge.auth.dto.RegisterRequest;
import com.apiforge.security.UserPrincipal;
import com.apiforge.user.PlatformUser;
import com.apiforge.user.PlatformUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final PlatformUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final long accessTokenTtlSeconds;

    public AuthService(
            PlatformUserRepository users,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtlSeconds = accessTokenTtl.toSeconds();
        if (this.accessTokenTtlSeconds < 60 || this.accessTokenTtlSeconds > 3600) {
            throw new IllegalArgumentException("JWT access-token TTL must be between 60 and 3600 seconds");
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validateBcryptPasswordLength(request.password());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        PlatformUser user = users.save(new PlatformUser(
                email, request.displayName().trim(), passwordEncoder.encode(request.password())));
        return tokenResponse(UserPrincipal.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            return tokenResponse((UserPrincipal) authentication.getPrincipal());
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    private AuthResponse tokenResponse(UserPrincipal user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenTtlSeconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("apiforge")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("roles", List.of(user.role().name()))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return new AuthResponse(token, "Bearer", accessTokenTtlSeconds,
                new AuthResponse.UserSummary(user.id(), user.email(), user.displayName(), user.role().name()));
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}