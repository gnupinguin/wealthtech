package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.SocialLink;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchEntity;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResult;
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
    private static final String CLIENT_RESULTS_UNAVAILABLE = "Client results are unavailable";
    private static final String DOCUMENT_RESULTS_UNAVAILABLE = "Document results are unavailable";

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

        var errors = new ArrayList<String>();
        var clientResultsUnavailable = clientFuture.isCompletedExceptionally();
        var documentResultsUnavailable = documentFuture.isCompletedExceptionally();
        if (clientResultsUnavailable) {
            log.error("Client search failed", clientFuture.exceptionNow());
            errors.add(CLIENT_RESULTS_UNAVAILABLE);
        }
        if (documentResultsUnavailable) {
            log.error("Document search failed", documentFuture.exceptionNow());
            errors.add(DOCUMENT_RESULTS_UNAVAILABLE);
        }

        checkAvailability(clientResultsUnavailable, documentResultsUnavailable);

        List<ClientSearchResult> clients = List.of();
        if (!clientResultsUnavailable) {
            try {
                clients = hydrateClients(clientFuture.getNow(List.of()));
            } catch (Exception e) {
                log.error("Client hydration failed", e);
                errors.add(CLIENT_RESULTS_UNAVAILABLE);
                clientResultsUnavailable = true;
            }
        }

        checkAvailability(clientResultsUnavailable, documentResultsUnavailable);
        return new SearchResult(clients, searchResultsOrEmpty(documentFuture), List.copyOf(errors));
    }

    private static void checkAvailability(boolean clientResultsUnavailable, boolean documentResultsUnavailable) {
        if (clientResultsUnavailable && documentResultsUnavailable) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service is unavailable");
        }
    }

    private @NonNull List<ClientSearchResult> hydrateClients(List<ClientSearchEntity> searchResults) {
        if (searchResults.isEmpty()) {
            return List.of();
        }

        var clientIds = searchResults.stream().map(ClientSearchEntity::id).toList();
        var clientEntities = clientRepository.findAllById(clientIds);
        var clientsById = StreamSupport.stream(clientEntities.spliterator(), false)
                .collect(Collectors.toMap(ClientEntity::id, client -> client));

        var clients = new ArrayList<ClientSearchResult>();
        for (var result : searchResults) {
            var client = clientsById.get(result.id());
            if (client == null) {
                log.warn("Client search returned missing client {}", result.id());
            } else {
                clients.add(toClientSearchResult(client, result.score()));
            }
        }
        return clients;
    }

    private static @NonNull ClientSearchResult toClientSearchResult(@NonNull ClientEntity client, float score) {
        var socialLinks = client.socialLinks().stream()
                .map(SocialLink::url)
                .toList();

        return new ClientSearchResult(
                client.id(),
                client.firstName(),
                client.lastName(),
                client.email(),
                client.description(),
                socialLinks,
                score
        );
    }

    private <T> @NonNull CompletableFuture<T> runAsync(long timeoutMillis, Supplier<T> search) {
        return CompletableFuture.supplyAsync(search, searchExecutor)
                .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private static <T> @NonNull List<T> searchResultsOrEmpty(@NonNull CompletableFuture<List<T>> future) {
        if (future.isCompletedExceptionally()) {
            return List.of();
        }
        return future.getNow(List.of());
    }

}
