package com.apiforge.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping("/api/v1/applications/{applicationId}/upstreams/{upstreamId}/routes")
public class ApiRouteController {

    private final ApiRouteService service;

    public ApiRouteController(ApiRouteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiRouteService.RouteResponse create(
            @PathVariable UUID applicationId, @PathVariable UUID upstreamId,
            @Valid @RequestBody CreateRouteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.create(UUID.fromString(jwt.getSubject()), applicationId, upstreamId,
                request.routeKey(), request.httpMethod(), request.upstreamPathPrefix());
    }

    @GetMapping
    public List<ApiRouteService.RouteResponse> list(
            @PathVariable UUID applicationId, @PathVariable UUID upstreamId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.list(UUID.fromString(jwt.getSubject()), applicationId, upstreamId);
    }

    @PatchMapping("/{routeId}/status")
    public ApiRouteService.RouteResponse updateStatus(
            @PathVariable UUID applicationId, @PathVariable UUID upstreamId,
            @PathVariable UUID routeId, @Valid @RequestBody UpdateRouteStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.updateStatus(UUID.fromString(jwt.getSubject()), applicationId,
                upstreamId, routeId, request.status());
    }

    public record CreateRouteRequest(
            @NotBlank @Size(min = 2, max = 63)
            @Pattern(regexp = "[A-Za-z][A-Za-z0-9-]{1,62}") String routeKey,
            @NotBlank @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD") String httpMethod,
            @Size(max = 500) String upstreamPathPrefix
    ) { }

    public record UpdateRouteStatusRequest(RouteStatus status) {
        public UpdateRouteStatusRequest {
            if (status == null) throw new IllegalArgumentException("status is required");
        }
    }
}
