package integration;

import io.gnupinguin.nevis.wealthtech.model.SocialLinkRequest;
import io.gnupinguin.nevis.wealthtech.rest.model.Client;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.model.SearchResponse;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchEntity;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchService;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SearchEndpointIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ClientSearchService clientSearchService;

    @MockitoBean
    private DocumentSearchService documentSearchService;

    @Test
    void testSearchReturnsDocumentsAndErrorWhenClientsUnavailable() {
        var documentId = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var document = new DocumentSearchResult(
                documentId,
                clientId,
                "Investment Policy",
                "Growth portfolio allocation",
                "Portfolio summary",
                0.91f
        );

        when(clientSearchService.search("growth", 10)).thenThrow(new IllegalStateException("clients unavailable"));
        when(documentSearchService.search("growth", 10)).thenReturn(List.of(document));

        var response = restTemplate.getForEntity("/search?q=growth", SearchResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertEquals("growth", body.query());
        assertThat(body.clients()).isEmpty();
        assertThat(body.documents()).singleElement()
                .satisfies(searchResult -> {
                    assertEquals(documentId, searchResult.id());
                    assertEquals(clientId, searchResult.clientId());
                    assertEquals("Investment Policy", searchResult.title());
                    assertEquals("Portfolio summary", searchResult.summary());
                    assertEquals("Growth portfolio allocation", searchResult.matchedChunk());
                });
        assertThat(body.errors()).containsExactly("Client results are unavailable");
    }

    @Test
    void testSearchReturnsClientsAndErrorWhenDocumentsUnavailable() {
        var client = createClient();

        when(clientSearchService.search("technology", 10)).thenReturn(List.of(new ClientSearchEntity(client.id(), 0.87f)));
        when(documentSearchService.search("technology", 10)).thenThrow(new IllegalStateException("documents unavailable"));

        var response = restTemplate.getForEntity("/search?q=technology", SearchResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertEquals("technology", body.query());
        assertThat(body.clients()).singleElement()
                .satisfies(searchResult -> {
                    assertEquals(client.id(), searchResult.id());
                    assertEquals("Ada", searchResult.firstName());
                    assertEquals("Lovelace", searchResult.lastName());
                    assertEquals("ada@example.com", searchResult.email());
                    assertEquals("Technology investor", searchResult.description());
                    assertThat(searchResult.socialLinks()).containsExactly("https://example.com/ada");
                });
        assertThat(body.documents()).isEmpty();
        assertThat(body.errors()).containsExactly("Document results are unavailable");
    }

    @Test
    void testSearchReturnsServiceUnavailableWhenClientsAndDocumentsUnavailable() {
        when(clientSearchService.search("growth", 10)).thenThrow(new IllegalStateException("clients unavailable"));
        when(documentSearchService.search("growth", 10)).thenThrow(new IllegalStateException("documents unavailable"));

        var response = restTemplate.getForEntity("/search?q=growth", String.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    private Client createClient() {
        var request = new CreateClientRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                "Technology investor",
                List.of(new SocialLinkRequest("https://example.com/ada"))
        );
        return restTemplate.postForObject("/clients", request, Client.class);
    }

}
