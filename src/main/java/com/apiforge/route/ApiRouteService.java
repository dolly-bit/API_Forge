package com.apiforge.route;

import com.apiforge.application.PlatformApplication;
import com.apiforge.application.PlatformApplicationRepository;
import com.apiforge.upstream.Upstream;
import com.apiforge.upstream.UpstreamRepository;
import com.apiforge.upstream.UpstreamStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ApiRouteService {

    private static final Set<String> SUPPORTED_METHODS =
            Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD);

    private final ApiRouteRepository routes;
    private final UpstreamRepository upstreams;
    private final PlatformApplicationRepository applications;

    public ApiRouteService(ApiRouteRepository routes, UpstreamRepository upstreams,
                           PlatformApplicationRepository applications) {
        this.routes = routes;
        this.upstreams = upstreams;
        this.applications = applications;
    }

    @Transactional
    public RouteResponse create(UUID ownerId, UUID applicationId, UUID upstreamId,
                                String routeKey, String httpMethod, String upstreamPathPrefix) {
        requireActiveOwnedApplication(ownerId, applicationId);
        Upstream upstream = upstreams.findByIdAndApplicationId(upstreamId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upstream not found"));
        if (upstream.getStatus() != UpstreamStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Upstream is not active");
        }

        String normalizedKey = normalizeRouteKey(routeKey);
        String normalizedMethod = httpMethod == null ? "" : httpMethod.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_METHODS.contains(normalizedMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HTTP method is not supported");
        }
        String normalizedPrefix = normalizePathPrefix(upstreamPathPrefix);

        ApiRoute saved = routes.save(new ApiRoute(applicationId, upstreamId, normalizedKey,
                normalizedMethod, normalizedPrefix));
        return RouteResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> list(UUID ownerId, UUID applicationId, UUID upstreamId) {
        requireOwnedApplication(ownerId, applicationId);
        requireUpstream(applicationId, upstreamId);
        return routes.findAllByApplicationIdAndUpstreamIdOrderByCreatedAtDesc(applicationId, upstreamId)
                .stream().map(RouteResponse::from).toList();
    }

    @Transactional
    public RouteResponse updateStatus(UUID ownerId, UUID applicationId, UUID upstreamId,
                                      UUID routeId, RouteStatus status) {
        requireOwnedApplication(ownerId, applicationId);
        requireUpstream(applicationId, upstreamId);
        ApiRoute route = routes.findByIdAndApplicationIdAndUpstreamId(
                        routeId, applicationId, upstreamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));
        route.setStatus(status);
        return RouteResponse.from(route);
    }

    private PlatformApplication requireOwnedApplication(UUID ownerId, UUID applicationId) {
        return applications.findByIdAndOwnerUserId(applicationId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private void requireActiveOwnedApplication(UUID ownerId, UUID applicationId) {
        if (!"ACTIVE".equals(requireOwnedApplication(ownerId, applicationId).getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not active");
        }
    }

    private Upstream requireUpstream(UUID applicationId, UUID upstreamId) {
        return upstreams.findByIdAndApplicationId(upstreamId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upstream not found"));
    }

    private String normalizeRouteKey(String value) {
        String key = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!key.matches("[a-z][a-z0-9-]{1,62}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Route key must be 2-63 lowercase letters, numbers, or hyphens and start with a letter");
        }
        return key;
    }

    private String normalizePathPrefix(String value) {
        if (value == null || value.isBlank() || "/".equals(value.trim())) return "";
        String prefix = value.trim();
        if (!prefix.startsWith("/") || prefix.contains("?") || prefix.contains("#")
                || prefix.contains("\\\\") || prefix.contains("/../")
                || prefix.endsWith("/..") || prefix.contains("/./")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upstream path prefix is invalid");
        }
        return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    public record RouteResponse(UUID id, UUID applicationId, UUID upstreamId, String routeKey,
                                String httpMethod, String upstreamPathPrefix, String status,
                                Instant createdAt, Instant updatedAt) {
        static RouteResponse from(ApiRoute route) {
            return new RouteResponse(route.getId(), route.getApplicationId(), route.getUpstreamId(),
                    route.getRouteKey(), route.getHttpMethod(), route.getUpstreamPathPrefix(),
                    route.getStatus().name(), route.getCreatedAt(), route.getUpdatedAt());
        }
    }
}
