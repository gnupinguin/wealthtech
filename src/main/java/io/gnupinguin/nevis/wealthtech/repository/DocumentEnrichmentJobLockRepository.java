package io.gnupinguin.nevis.wealthtech.repository;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentEnrichmentJobLockRepository {

    private static final String LOCK_NEXT_PENDING_JOB = """
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
            """;

    private final JdbcClient jdbcClient;

    public DocumentEnrichmentJobLockRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public Optional<DocumentEnrichmentJobEntity> tryLockNextPendingJob() {
        return jdbcClient.sql(LOCK_NEXT_PENDING_JOB)
                .query(this::mapRow)
                .optional();
    }

    private DocumentEnrichmentJobEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentEnrichmentJobEntity(
                rs.getObject("id", UUID.class),
                rs.getObject("document_id", UUID.class),
                JobType.valueOf(rs.getString("type")),
                JobStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                rs.getInt("max_attempts"),
                rs.getString("last_error"),
                toInstant(rs.getObject("available_at", OffsetDateTime.class)),
                toInstant(rs.getObject("locked_at", OffsetDateTime.class)),
                toInstant(rs.getObject("completed_at", OffsetDateTime.class)),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class))
        );
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }
}
