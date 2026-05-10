package io.gnupinguin.nevis.wealthtech.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("document_enrichment_outbox_events")
public record DocumentEnrichmentOutboxEventEntity(
        @Id UUID id,
        UUID documentId,
        JobType type,
        OutboxEventStatus status,
        int attempts,
        String lastError,
        Instant availableAt,
        Instant lockedAt,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {}
