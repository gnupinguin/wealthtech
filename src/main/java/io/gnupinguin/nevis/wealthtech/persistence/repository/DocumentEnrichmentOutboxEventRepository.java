package io.gnupinguin.nevis.wealthtech.persistence.repository;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentOutboxEventEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.entity.OutboxEventStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentEnrichmentOutboxEventRepository extends CrudRepository<DocumentEnrichmentOutboxEventEntity, UUID> {

    @Query("""
            UPDATE document_enrichment_outbox_events
            SET status     = 'PROCESSING',
                locked_at  = NOW(),
                attempts   = attempts + 1,
                updated_at = NOW()
            WHERE id IN (
                SELECT id
                FROM document_enrichment_outbox_events
                WHERE (
                    status = 'PENDING'
                    AND available_at <= NOW()
                ) OR (
                    status = 'PROCESSING'
                    AND locked_at <= NOW() - (:lockTimeoutMs * INTERVAL '1 millisecond')
                )
                ORDER BY available_at, created_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
            """)
    @NonNull
    List<DocumentEnrichmentOutboxEventEntity> lockNextPublishableEvents(
            @Param("limit") int limit,
            @Param("lockTimeoutMs") long lockTimeoutMs);

    @Modifying
    @Query("""
            UPDATE document_enrichment_outbox_events
            SET status       = 'PUBLISHED',
                last_error   = NULL,
                locked_at    = NULL,
                published_at = NOW(),
                updated_at   = NOW()
            WHERE id = :id
            """)
    int markPublished(@Param("id") @NonNull UUID id);

    @Modifying
    @Query("""
            UPDATE document_enrichment_outbox_events
            SET status       = 'PENDING',
                last_error   = :error,
                available_at = :availableAt,
                locked_at    = NULL,
                updated_at   = NOW()
            WHERE id = :id
            """)
    int markPublishFailed(
            @Param("id") @NonNull UUID id,
            @Param("error") @NonNull String error,
            @Param("availableAt") @NonNull Instant availableAt);

    long countByStatusAndType(@NonNull OutboxEventStatus status, @NonNull JobType type);

}
