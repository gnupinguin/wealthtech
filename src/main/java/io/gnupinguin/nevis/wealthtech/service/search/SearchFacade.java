package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.config.SearchProperties;
import io.gnupinguin.nevis.wealthtech.exception.ServiceUnavailableException;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResultHydrator;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchService;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class SearchFacade {

    private static final String CLIENT_RESULTS_UNAVAILABLE = "Client results are unavailable";
    private static final String DOCUMENT_RESULTS_UNAVAILABLE = "Document results are unavailable";

    private final ClientSearchService clientSearchService;
    private final DocumentSearchService documentSearchService;
    private final ClientSearchResultHydrator clientSearchResultHydrator;
    private final SearchProperties searchProperties;

    private final Executor searchExecutor;

    public SearchFacade(ClientSearchService clientSearchService,
                        DocumentSearchService documentSearchService,
                        ClientSearchResultHydrator clientSearchResultHydrator,
                        SearchProperties searchProperties,
                        @Qualifier("searchExecutor") Executor searchExecutor) {
        this.clientSearchService = clientSearchService;
        this.documentSearchService = documentSearchService;
        this.clientSearchResultHydrator = clientSearchResultHydrator;
        this.searchProperties = searchProperties;
        this.searchExecutor = searchExecutor;
    }

    public @NonNull SearchResult search(@NonNull String query, int clientLimit, int documentLimit) {
        var clientFuture = runAsync(
                searchProperties.clientTimeoutMs(),
                () -> clientSearchService.search(query, clientLimit)
        );
        var documentFuture = runAsync(
                searchProperties.documentTimeoutMs(),
                () -> documentSearchService.search(query, documentLimit)
        );

        awaitAll(clientFuture, documentFuture);

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

        throwIfAllResultsUnavailable(clientResultsUnavailable, documentResultsUnavailable);

        List<ClientSearchResult> clients = List.of();
        if (!clientResultsUnavailable) {
            try {
                clients = clientSearchResultHydrator.hydrate(clientFuture.getNow(List.of()));
            } catch (Exception e) {
                log.warn("Client hydration failed", e);
                errors.add(CLIENT_RESULTS_UNAVAILABLE);
                clientResultsUnavailable = true;
            }
        }

        throwIfAllResultsUnavailable(clientResultsUnavailable, documentResultsUnavailable);
        return new SearchResult(clients, searchResultsOrEmpty(documentFuture), List.copyOf(errors));
    }

    private static void throwIfAllResultsUnavailable(boolean clientResultsUnavailable, boolean documentResultsUnavailable) {
        if (clientResultsUnavailable && documentResultsUnavailable) {
            throw new ServiceUnavailableException("Service is unavailable");
        }
    }

    private static void awaitAll(CompletableFuture<?> first, CompletableFuture<?> second) {
        CompletableFuture.allOf(first, second).exceptionally(e -> null).join();
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
