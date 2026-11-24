package com.apiforge.route;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_routes")
public class ApiRoute {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "upstream_id", nullable = false)
    private UUID upstreamId;

    @Column(name = "route_key", nullable = false, length = 63)
    private String routeKey;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "upstream_path_prefix", nullable = false, length = 500)
    private String upstreamPathPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStatus status = RouteStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApiRoute() { }

    public ApiRoute(UUID applicationId, UUID upstreamId, String routeKey,
                    String httpMethod, String upstreamPathPrefix) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.upstreamId = upstreamId;
        this.routeKey = routeKey;
        this.httpMethod = httpMethod;
        this.upstreamPathPrefix = upstreamPathPrefix;
    }

    public void setStatus(RouteStatus status) {
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getApplicationId() { return applicationId; }
    public UUID getUpstreamId() { return upstreamId; }
    public String getRouteKey() { return routeKey; }
    public String getHttpMethod() { return httpMethod; }
    public String getUpstreamPathPrefix() { return upstreamPathPrefix; }
    public RouteStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
