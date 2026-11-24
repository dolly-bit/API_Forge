package com.apiforge.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final PlatformUserRepository users;

    public UserController(PlatformUserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PlatformUser user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole().name(), user.getCreatedAt());
    }

    public record CurrentUserResponse(
            UUID id, String email, String displayName, String role, java.time.Instant createdAt) { }
}