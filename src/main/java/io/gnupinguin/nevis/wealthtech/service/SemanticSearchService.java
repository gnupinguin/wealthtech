package io.gnupinguin.nevis.wealthtech.service;

import io.gnupinguin.nevis.wealthtech.model.ClientScoreResult;
import io.gnupinguin.nevis.wealthtech.model.ClientSearchResult;
import io.gnupinguin.nevis.wealthtech.model.DocumentSearchResult;
import io.gnupinguin.nevis.wealthtech.model.SearchResponse;
import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.SocialLink;
import io.gnupinguin.nevis.wealthtech.repository.SqlQueryHelper;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.repository.DocumentChunkSearchRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SemanticSearchService {

    private static final int DEFAULT_LIMIT = 10;

    private final EmbeddingModel embeddingModel;
    private final DocumentChunkSearchRepository searchRepository;
    private final ClientRepository clientRepository;
    private final Executor searchExecutor;

    public SemanticSearchService(EmbeddingModel embeddingModel,
                                 DocumentChunkSearchRepository searchRepository,
                                 ClientRepository clientRepository,
                                 @Qualifier("searchExecutor") Executor searchExecutor) {
        this.embeddingModel = embeddingModel;
        this.searchRepository = searchRepository;
        this.clientRepository = clientRepository;
        this.searchExecutor = searchExecutor;
    }

    public SearchResponse search(String query) {
        var matchFuture =
                CompletableFuture.supplyAsync(
                        () -> clientRepository.findMatching(query, DEFAULT_LIMIT),
                        searchExecutor);

        var semanticFuture =
                CompletableFuture.supplyAsync(() -> {
                    float[] embedding = embeddingModel.embed(query);
                    return searchRepository.findSimilar(SqlQueryHelper.toVectorString(embedding), DEFAULT_LIMIT);
                }, searchExecutor);

        CompletableFuture.allOf(matchFuture, semanticFuture).join();

        var documents = semanticFuture.join();
        var mergedScores = mergeClientScores(deriveClientScores(documents), matchFuture.join());
        var clients = hydrateClients(mergedScores);

        return new SearchResponse(query, clients, documents);
    }

    private Map<UUID, Float> deriveClientScores(List<DocumentSearchResult> documents) {
        return documents.stream()
                .collect(Collectors.toMap(
                        DocumentSearchResult::clientId,
                        DocumentSearchResult::score,
                        Math::max
                ));
    }

    private Map<UUID, Float> mergeClientScores(Map<UUID, Float> semantic, List<ClientScoreResult> match) {
        Map<UUID, Float> merged = new HashMap<>(semantic);
        match.forEach(r -> merged.merge(r.id(), r.score(), Math::max));
        return merged;
    }

    private List<ClientSearchResult> hydrateClients(Map<UUID, Float> scores) {
        Map<UUID, ClientEntity> entities = StreamSupport
                .stream(clientRepository.findAllById(scores.keySet()).spliterator(), false)
                .collect(Collectors.toMap(ClientEntity::id, Function.identity()));

        return scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(e -> {
                    ClientEntity entity = entities.get(e.getKey());
                    List<String> links = entity.socialLinks().stream()
                            .map(SocialLink::url)
                            .toList();
                    return new ClientSearchResult(
                            entity.id(), entity.firstName(), entity.lastName(),
                            entity.email(), entity.description(), links, e.getValue());
                })
                .toList();
    }

}
