package io.gnupinguin.nevis.wealthtech.repository;

import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkSearchRepository extends Repository<DocumentSearchEntity, UUID> {

    @Query("""
            WITH ranked_chunks AS (
                SELECT
                    d.id,
                    d.client_id,
                    d.title,
                    d.summary,
                    d.created_at,
                    dc.content AS matched_chunk,
                    1 - (dc.embedding <=> :vector::vector) AS score,
                    ROW_NUMBER() OVER (
                        PARTITION BY d.id
                        ORDER BY dc.embedding <=> :vector::vector
                    ) AS rn
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
            )
            SELECT
                id,
                client_id,
                title,
                summary,
                created_at,
                matched_chunk,
                score
            FROM ranked_chunks
            WHERE rn = 1
            ORDER BY score DESC
            LIMIT :limit;
            """)
    List<DocumentSearchEntity> findSimilar(@Param("vector") String queryEmbedding, @Param("limit") int limit);

}
