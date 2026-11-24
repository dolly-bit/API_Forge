package com.apiforge.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final PlatformApplicationRepository applications;

    public ApplicationController(PlatformApplicationRepository applications) {
        this.applications = applications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody CreateApplicationRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        PlatformApplication application = applications.save(new PlatformApplication(
                ownerId, request.name().trim(), request.description()));
        return ApplicationResponse.from(application);
    }

    @GetMapping
    public List<ApplicationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return applications.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerId)
                .stream().map(ApplicationResponse::from).toList();
    }

    public record CreateApplicationRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @Size(max = 500) String description
    ) { }

    public record ApplicationResponse(
            UUID id, String name, String description, String status, Instant createdAt) {
        static ApplicationResponse from(PlatformApplication application) {
            return new ApplicationResponse(application.getId(), application.getName(),
                    application.getDescription(), application.getStatus(), application.getCreatedAt());
        }
    }
}