package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.service.search.SearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchResponseMapperTest {

    private final SearchResponseMapper mapper = new SearchResponseMapper();

    @Test
    void testToResponsePreservesQueryString() {
        var result = new SearchResult(List.of(), List.of(), List.of());

        var response = mapper.toResponse("growth", result);

        assertThat(response.query()).isEqualTo("growth");
    }

    @Test
    void testToResponseMapsClientsToClientSearchResponses() {
        var clientId = UUID.randomUUID();
        var client = new ClientSearchResult(clientId, "Ada", "Lovelace", "ada@example.com", "Investor",
                List.of("https://example.com/ada"), 0.91f);
        var result = new SearchResult(List.of(client), List.of(), List.of());

        var response = mapper.toResponse("ada", result);

        assertThat(response.clients()).singleElement()
                .satisfies(c -> {
                    assertThat(c.id()).isEqualTo(clientId);
                    assertThat(c.firstName()).isEqualTo("Ada");
                    assertThat(c.lastName()).isEqualTo("Lovelace");
                    assertThat(c.email()).isEqualTo("ada@example.com");
                    assertThat(c.description()).isEqualTo("Investor");
                    assertThat(c.socialLinks()).containsExactly("https://example.com/ada");
                    assertThat(c.score()).isEqualTo(0.91f);
                });
    }

    @Test
    void testToResponseMapsDocumentsToDocumentSearchResponses() {
        var documentId = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var document = new DocumentSearchResult(documentId, clientId, "Portfolio Q1", "Growth allocation", "Summary", 0.87f);
        var result = new SearchResult(List.of(), List.of(document), List.of());

        var response = mapper.toResponse("growth", result);

        assertThat(response.documents()).singleElement()
                .satisfies(d -> {
                    assertThat(d.id()).isEqualTo(documentId);
                    assertThat(d.clientId()).isEqualTo(clientId);
                    assertThat(d.title()).isEqualTo("Portfolio Q1");
                    assertThat(d.matchedChunk()).isEqualTo("Growth allocation");
                    assertThat(d.summary()).isEqualTo("Summary");
                    assertThat(d.score()).isEqualTo(0.87f);
                });
    }

    @Test
    void testToResponsePreservesErrors() {
        var result = new SearchResult(List.of(), List.of(), List.of("Client results are unavailable"));

        var response = mapper.toResponse("growth", result);

        assertThat(response.errors()).containsExactly("Client results are unavailable");
    }

    @Test
    void testToResponseReturnsEmptyListsWhenResultIsEmpty() {
        var result = new SearchResult(List.of(), List.of(), List.of());

        var response = mapper.toResponse("growth", result);

        assertThat(response.clients()).isEmpty();
        assertThat(response.documents()).isEmpty();
        assertThat(response.errors()).isEmpty();
    }

}
