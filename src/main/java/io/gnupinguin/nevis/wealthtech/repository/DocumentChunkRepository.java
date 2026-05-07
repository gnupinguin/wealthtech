package io.gnupinguin.nevis.wealthtech.repository;

import io.gnupinguin.nevis.wealthtech.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentChunkRepository {

    private static final String INSERT_SQL =
            "INSERT INTO document_chunks (id, document_id, chunk_index, content, embedding, created_at) " +
            "VALUES (gen_random_uuid(), :documentId, :chunkIndex, :content, :embedding::vector, :createdAt) " +
            "ON CONFLICT (document_id, chunk_index) DO UPDATE SET content = EXCLUDED.content, embedding = EXCLUDED.embedding";

    private final NamedParameterJdbcTemplate jdbc;


    public void saveAll(List<DocumentChunk> chunks) {
        Instant now = Instant.now();
        var params = chunks.stream()
                .map(chunk -> new MapSqlParameterSource()
                        .addValue("documentId", chunk.documentId())
                        .addValue("chunkIndex", chunk.chunkIndex())
                        .addValue("content", chunk.content())
                        .addValue("embedding", SqlQueryHelper.toVectorString(chunk.embedding()))
                        .addValue("createdAt", Timestamp.from(now)))
                .toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate(INSERT_SQL, params);
    }

}
