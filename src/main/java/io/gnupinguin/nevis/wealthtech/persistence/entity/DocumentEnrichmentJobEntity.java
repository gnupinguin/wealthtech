package io.gnupinguin.nevis.wealthtech.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("document_enrichment_jobs")
public record DocumentEnrichmentJobEntity(
        @Id UUID id,
        UUID documentId,
        JobType type,
        JobStatus status,
        int attempts,
        int maxAttempts,
        String lastError,
        Instant availableAt,
        Instant lockedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {}
