package io.gnupinguin.nevis.wealthtech.persistence.repository;

import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class DocumentEnrichmentProcessedEventRepository {

    private static final String INSERT_SQL = """
            INSERT INTO document_enrichment_processed_events (
                event_id,
                document_id,
                type,
                processed_at
            )
            VALUES (
                :eventId,
                :documentId,
                :type,
                :processedAt
            )
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public boolean recordProcessingStarted(@NonNull DocumentEnrichmentEvent event) {
        var params = new MapSqlParameterSource()
                .addValue("eventId", event.id())
                .addValue("documentId", event.documentId())
                .addValue("type", event.type().name())
                .addValue("processedAt", Timestamp.from(Instant.now()));

        return jdbc.update(INSERT_SQL, params) == 1;
    }

}
