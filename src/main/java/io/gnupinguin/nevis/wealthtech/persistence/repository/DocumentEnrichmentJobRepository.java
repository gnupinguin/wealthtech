package io.gnupinguin.nevis.wealthtech.persistence.repository;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentEnrichmentJobRepository extends CrudRepository<DocumentEnrichmentJobEntity, UUID> {

    @Query("""
            UPDATE document_enrichment_jobs
            SET status     = 'PROCESSING',
                locked_at  = NOW(),
                attempts   = attempts + 1,
                updated_at = NOW()
            WHERE id IN (
                SELECT id
                FROM document_enrichment_jobs
                WHERE status = 'PENDING'
                  AND available_at <= NOW()
                  AND attempts < max_attempts
                ORDER BY available_at, created_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
            """)
    @NonNull
    List<DocumentEnrichmentJobEntity> tryLockNextPendingJobs(@Param("limit") int limit);

    long countByStatusAndType(@NonNull JobStatus status, @NonNull JobType type);

}
