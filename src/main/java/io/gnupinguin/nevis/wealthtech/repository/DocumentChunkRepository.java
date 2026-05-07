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

@Repository
@RequiredArgsConstructor
public class DocumentChunkRepository {

    private static final String INSERT_SQL = """
            INSERT INTO document_chunks (
                        id,
                        document_id,
                        chunk_index,
                        content,
                        embedding,
                        created_at
                    )
                    VALUES (
                        gen_random_uuid(),
                        :documentId,
                        :chunkIndex,
                        :content,
                        CAST(:embedding AS vector),
                        :createdAt
                    )
            """;

    private final NamedParameterJdbcTemplate jdbc;


    @Transactional
    public void saveAll(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        Timestamp createdAt = Timestamp.from(Instant.now());

        var params = chunks.stream()
                .map(chunk -> new MapSqlParameterSource()
                        .addValue("documentId", chunk.documentId())
                        .addValue("chunkIndex", chunk.chunkIndex())
                        .addValue("content", chunk.content())
                        .addValue("embedding", SqlQueryHelper.toVectorString(chunk.embedding()))
                        .addValue("createdAt", createdAt))
                .toArray(MapSqlParameterSource[]::new);

        jdbc.batchUpdate(INSERT_SQL, params);
    }

}
