package com.apiforge.apikey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findAllByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
    Optional<ApiKey> findByIdAndApplicationId(UUID id, UUID applicationId);
    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);
}