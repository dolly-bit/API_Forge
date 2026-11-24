package com.apiforge.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformApplicationRepository extends JpaRepository<PlatformApplication, UUID> {
    List<PlatformApplication> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);
    Optional<PlatformApplication> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}