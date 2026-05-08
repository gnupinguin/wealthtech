package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.rest.model.ClientSearchResponse;
import io.gnupinguin.nevis.wealthtech.rest.model.DocumentSearchResponse;
import io.gnupinguin.nevis.wealthtech.rest.model.SearchResponse;
import io.gnupinguin.nevis.wealthtech.service.search.SearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchResult;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchResponseMapper {

    public @NonNull SearchResponse toResponse(@NonNull String query, @NonNull SearchResult result) {
        var clients = convertClients(result);
        var documents = convertDocuments(result);

        return new SearchResponse(query, clients, documents, result.errors());
    }

    private static @NonNull List<DocumentSearchResponse> convertDocuments(@NonNull SearchResult result) {
        return result.documents().stream()
                .map(SearchResponseMapper::convertDocument)
                .toList();
    }

    private static @NonNull List<ClientSearchResponse> convertClients(@NonNull SearchResult result) {
        return result.clients().stream()
                .map(SearchResponseMapper::convertClient)
                .toList();
    }

    private static @NonNull DocumentSearchResponse convertDocument(@NonNull DocumentSearchResult document) {
        return new DocumentSearchResponse(
                document.id(),
                document.clientId(),
                document.score(),
                document.title(),
                document.summary(),
                document.matchedChunk()
        );
    }

    private static @NonNull ClientSearchResponse convertClient(@NonNull ClientSearchResult client) {
        return new ClientSearchResponse(
                client.id(),
                client.firstName(),
                client.lastName(),
                client.email(),
                client.description(),
                client.socialLinks(),
                client.score()
        );
    }

}
