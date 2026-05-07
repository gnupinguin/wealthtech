package io.gnupinguin.nevis.wealthtech.repository;

import io.gnupinguin.nevis.wealthtech.model.DocumentSearchResult;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkSearchRepository extends Repository<DocumentSearchResult, UUID> {

    @Query("""
            SELECT d.id, d.client_id, d.title, d.summary, dc.content as matchedChunk,
                   (1 - (dc.embedding <=> :vector::vector)) AS score
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            ORDER BY dc.embedding <=> :vector::vector
            LIMIT :limit
            """)
    List<DocumentSearchResult> findSimilar(@Param("vector") String queryEmbedding, @Param("limit") int limit);

}
