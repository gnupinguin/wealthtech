package io.gnupinguin.nevis.wealthtech.service.search.document;

import io.gnupinguin.nevis.wealthtech.repository.DocumentChunkSearchRepository;
import io.gnupinguin.nevis.wealthtech.repository.SqlQueryHelper;
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
    public @NonNull List<DocumentSearchEntity> search(@NonNull String query, int limit) {
        var searchVector = embeddingModel.embed(query); // TODO wrap client errors
        return repository.findSimilar(SqlQueryHelper.toVectorString(searchVector), limit);
    }

}
