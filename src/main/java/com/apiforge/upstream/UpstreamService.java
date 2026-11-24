package com.apiforge.upstream;

import com.apiforge.application.PlatformApplication;
import com.apiforge.application.PlatformApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UpstreamService {

    private final UpstreamRepository upstreams;
    private final PlatformApplicationRepository applications;
    private final UpstreamUrlPolicy urlPolicy;

    public UpstreamService(UpstreamRepository upstreams,
                           PlatformApplicationRepository applications,
                           UpstreamUrlPolicy urlPolicy) {
        this.upstreams = upstreams;
        this.applications = applications;
        this.urlPolicy = urlPolicy;
    }

    @Transactional
    public UpstreamResponse create(UUID ownerId, UUID applicationId, String name, String baseUrl) {
        requireActiveOwnedApplication(ownerId, applicationId);
        String normalizedUrl = urlPolicy.validateAndNormalize(baseUrl);
        Upstream saved = upstreams.save(new Upstream(applicationId, name.trim(), normalizedUrl));
        return UpstreamResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<UpstreamResponse> list(UUID ownerId, UUID applicationId) {
        requireOwnedApplication(ownerId, applicationId);
        return upstreams.findAllByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream().map(UpstreamResponse::from).toList();
    }

    @Transactional
    public UpstreamResponse updateStatus(UUID ownerId, UUID applicationId,
                                         UUID upstreamId, UpstreamStatus status) {
        requireOwnedApplication(ownerId, applicationId);
        Upstream upstream = upstreams.findByIdAndApplicationId(upstreamId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upstream not found"));
        upstream.setStatus(status);
        return UpstreamResponse.from(upstream);
    }

    private void requireActiveOwnedApplication(UUID ownerId, UUID applicationId) {
        PlatformApplication application = requireOwnedApplication(ownerId, applicationId);
        if (!"ACTIVE".equals(application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not active");
        }
    }

    private PlatformApplication requireOwnedApplication(UUID ownerId, UUID applicationId) {
        return applications.findByIdAndOwnerUserId(applicationId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    public record UpstreamResponse(UUID id, UUID applicationId, String name, String baseUrl,
                                   String status, Instant createdAt, Instant updatedAt) {
        static UpstreamResponse from(Upstream upstream) {
            return new UpstreamResponse(upstream.getId(), upstream.getApplicationId(),
                    upstream.getName(), upstream.getBaseUrl(), upstream.getStatus().name(),
                    upstream.getCreatedAt(), upstream.getUpdatedAt());
        }
    }
}
