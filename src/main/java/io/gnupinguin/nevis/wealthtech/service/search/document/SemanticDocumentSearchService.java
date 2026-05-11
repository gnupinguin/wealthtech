package io.gnupinguin.nevis.wealthtech.service.search.document;

import io.gnupinguin.nevis.wealthtech.exception.ServiceUnavailableException;
import io.gnupinguin.nevis.wealthtech.persistence.projection.DocumentSearchProjection;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentChunkSearchRepository;
import io.gnupinguin.nevis.wealthtech.persistence.repository.SqlQueryHelper;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderException;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderGuard;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticDocumentSearchService implements DocumentSearchService {

    private final DocumentChunkSearchRepository repository;
    private final EmbeddingModel embeddingModel;

    @Override
    public @NonNull List<DocumentSearchResult> search(@NonNull String query, int limit) {
        var searchVector = searchVector(query);
        return repository.findSimilar(SqlQueryHelper.toVectorString(searchVector), limit).stream()
                .map(SemanticDocumentSearchService::toResult)
                .toList();
    }

    private float @NonNull [] searchVector(@NonNull String query) {
        try {
            return AiProviderGuard.requireEmbedding(
                    AiProviderOperation.EMBEDDING_SEARCH,
                    AiProviderGuard.call(AiProviderOperation.EMBEDDING_SEARCH, () -> embeddingModel.embed(query)),
                    "empty search embedding"
            );
        } catch (AiProviderException e) {
            log.error(
                    "Document search AI provider failed: operation={}, type={}, detail={}",
                    e.operation().label(),
                    e.type(),
                    e.detail()
            );
            throw new ServiceUnavailableException("Third party service is unavailable", e);
        }
    }

    private static @NonNull DocumentSearchResult toResult(@NonNull DocumentSearchProjection entity) {
        return new DocumentSearchResult(
                entity.id(),
                entity.clientId(),
                entity.title(),
                entity.matchedChunk(),
                entity.summary(),
                entity.score()
        );
    }

}
