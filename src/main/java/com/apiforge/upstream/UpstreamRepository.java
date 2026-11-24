package com.apiforge.upstream;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UpstreamRepository extends JpaRepository<Upstream, UUID> {
    List<Upstream> findAllByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
    Optional<Upstream> findByIdAndApplicationId(UUID id, UUID applicationId);
}
