package com.apiforge.route;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiRouteRepository extends JpaRepository<ApiRoute, UUID> {
    List<ApiRoute> findAllByApplicationIdAndUpstreamIdOrderByCreatedAtDesc(
            UUID applicationId, UUID upstreamId);
    Optional<ApiRoute> findByIdAndApplicationIdAndUpstreamId(
            UUID id, UUID applicationId, UUID upstreamId);
}
