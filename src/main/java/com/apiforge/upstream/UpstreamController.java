package com.apiforge.upstream;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/upstreams")
public class UpstreamController {

    private final UpstreamService service;

    public UpstreamController(UpstreamService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UpstreamService.UpstreamResponse create(
            @PathVariable UUID applicationId,
            @Valid @RequestBody CreateUpstreamRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.create(UUID.fromString(jwt.getSubject()), applicationId,
                request.name(), request.baseUrl());
    }

    @GetMapping
    public List<UpstreamService.UpstreamResponse> list(
            @PathVariable UUID applicationId, @AuthenticationPrincipal Jwt jwt) {
        return service.list(UUID.fromString(jwt.getSubject()), applicationId);
    }

    @PatchMapping("/{upstreamId}/status")
    public UpstreamService.UpstreamResponse updateStatus(
            @PathVariable UUID applicationId, @PathVariable UUID upstreamId,
            @Valid @RequestBody UpdateUpstreamStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.updateStatus(UUID.fromString(jwt.getSubject()), applicationId,
                upstreamId, request.status());
    }

    public record CreateUpstreamRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotBlank @Size(max = 500) String baseUrl
    ) { }

    public record UpdateUpstreamStatusRequest(@NotNull UpstreamStatus status) { }
}
