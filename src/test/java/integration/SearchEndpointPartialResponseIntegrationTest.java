package integration;

import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import io.gnupinguin.nevis.wealthtech.rest.dto.ClientResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.SearchResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.SocialLinkRequest;
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

class SearchEndpointPartialResponseIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ClientSearchService clientSearchService;

    @MockitoBean
    private DocumentSearchService documentSearchService;

    @Test
    void testSearchReturnsDocumentsAndErrorWhenClientSearchFails() {
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
        when(clientSearchService.search("growth", 5)).thenThrow(new IllegalStateException("clients unavailable"));
        when(documentSearchService.search("growth", 10)).thenReturn(List.of(document));

        var response = restTemplate.getForEntity("/search?q=growth", SearchResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertEquals("growth", body.query());
        assertThat(body.clients()).isEmpty();
        assertThat(body.documents()).singleElement()
                .satisfies(d -> {
                    assertEquals(documentId, d.id());
                    assertEquals(clientId, d.clientId());
                    assertEquals("Investment Policy", d.title());
                    assertEquals("Portfolio summary", d.summary());
                    assertEquals("Growth portfolio allocation", d.matchedChunk());
                });
        assertThat(body.errors()).containsExactly("Client results are unavailable");
    }

    @Test
    void testSearchReturnsClientsAndErrorWhenDocumentSearchFails() {
        var client = createClient();
        when(clientSearchService.search("technology", 5)).thenReturn(List.of(new ClientSearchProjection(client.id(), 0.87f)));
        when(documentSearchService.search("technology", 10)).thenThrow(new IllegalStateException("documents unavailable"));

        var response = restTemplate.getForEntity("/search?q=technology", SearchResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertEquals("technology", body.query());
        assertThat(body.clients()).singleElement()
                .satisfies(c -> {
                    assertEquals(client.id(), c.id());
                    assertEquals("Ada", c.firstName());
                    assertEquals("Lovelace", c.lastName());
                    assertEquals("ada@example.com", c.email());
                    assertEquals("Technology investor", c.description());
                    assertThat(c.socialLinks()).containsExactly("https://example.com/ada");
                });
        assertThat(body.documents()).isEmpty();
        assertThat(body.errors()).containsExactly("Document results are unavailable");
    }

    private ClientResponse createClient() {
        var request = new CreateClientRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                "Technology investor",
                List.of(new SocialLinkRequest("https://example.com/ada"))
        );
        return restTemplate.postForObject("/clients", request, ClientResponse.class);
    }

}
