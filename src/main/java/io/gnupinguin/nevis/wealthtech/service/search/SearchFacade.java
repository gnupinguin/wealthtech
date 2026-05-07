package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchEntity;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchService;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class SearchFacade {

    private static final int DEFAULT_LIMIT = 10;

    private final ClientRepository clientRepository;
    private final ClientSearchService clientSearchService;
    private final DocumentSearchService documentSearchService;

    private final Executor searchExecutor;

    public SearchFacade(ClientRepository clientRepository,
                        ClientSearchService clientSearchService,
                        DocumentSearchService documentSearchService,
                        @Qualifier("searchExecutor") Executor searchExecutor) {
        this.clientRepository = clientRepository;
        this.clientSearchService = clientSearchService;
        this.documentSearchService = documentSearchService;
        this.searchExecutor = searchExecutor;
    }

    public @NonNull SearchResult search(@NonNull String query) {
        var clientFuture = runAsync(10_000, () -> clientSearchService.search(query, DEFAULT_LIMIT));
        var documentFuture = runAsync(30_000, () -> documentSearchService.search(query, DEFAULT_LIMIT));

        CompletableFuture.allOf(clientFuture, documentFuture).exceptionally(e -> null).join();

        if (clientFuture.isCompletedExceptionally()) {
            log.error("Client search failed", clientFuture.exceptionNow());
        }
        if (documentFuture.isCompletedExceptionally()) {
            log.error("Document search failed", documentFuture.exceptionNow());
        }

        if (clientFuture.isCompletedExceptionally() && documentFuture.isCompletedExceptionally()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service is unavailable");
        }

        var scoredEntities = hydrateClients(clientFuture.getNow(List.of()));
        return new SearchResult(scoredEntities, documentFuture.getNow(List.of()));
    }

    private @NonNull List<ScoredEntity<ClientEntity>> hydrateClients(List<ClientSearchEntity> searchResults) {
        if (searchResults.isEmpty()) {
            return List.of();
        }

        var clientIds = searchResults.stream().map(ClientSearchEntity::id).toList();
        var clients = clientRepository.findAllById(clientIds);
        var clientsById = StreamSupport.stream(clients.spliterator(), false)
                .collect(Collectors.toMap(ClientEntity::id, client -> client));

        var scoredEntities = new ArrayList<ScoredEntity<ClientEntity>>();
        for (var result : searchResults) {
            var client = clientsById.get(result.id());
            if (client == null) {
                log.warn("Client search returned missing client {}", result.id());
            } else {
                scoredEntities.add(new ScoredEntity<>(client, result.score()));
            }
        }
        return scoredEntities;
    }

    private <T> @NonNull CompletableFuture<T> runAsync(long timeoutMillis, Supplier<T> search) {
        return CompletableFuture.supplyAsync(search, searchExecutor)
                .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
    }

}
