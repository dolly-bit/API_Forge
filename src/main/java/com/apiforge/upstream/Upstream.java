package com.apiforge.upstream;

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
@Table(name = "upstreams")
public class Upstream {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UpstreamStatus status = UpstreamStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Upstream() { }

    public Upstream(UUID applicationId, String name, String baseUrl) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.name = name;
        this.baseUrl = baseUrl;
    }

    public void setStatus(UpstreamStatus status) {
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
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public UpstreamStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
