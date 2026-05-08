package io.gnupinguin.nevis.wealthtech.persistence.repository;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentEnrichmentJobRepository extends CrudRepository<DocumentEnrichmentJobEntity, UUID> {

    @Query("""
            UPDATE document_enrichment_jobs
            SET status     = 'PROCESSING',
                locked_at  = NOW(),
                attempts   = attempts + 1,
                updated_at = NOW()
            WHERE id = (
                SELECT id
                FROM document_enrichment_jobs
                WHERE status = 'PENDING'
                  AND available_at <= NOW()
                  AND attempts < max_attempts
                ORDER BY available_at, created_at
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
            """)
    Optional<DocumentEnrichmentJobEntity> tryLockNextPendingJob();

}
