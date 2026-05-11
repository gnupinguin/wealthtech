package integration;

import io.gnupinguin.nevis.wealthtech.rest.dto.SearchResponse;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchService;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchEndpointIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ClientSearchService clientSearchService;

    @MockitoBean
    private DocumentSearchService documentSearchService;

    @Test
    void testSearchReturnsServiceUnavailableWhenClientsAndDocumentsUnavailable() {
        when(clientSearchService.search("growth", 5)).thenThrow(new IllegalStateException("clients unavailable"));
        when(documentSearchService.search("growth", 10)).thenThrow(new IllegalStateException("documents unavailable"));

        var response = restTemplate.getForEntity("/search?q=growth", String.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void testSearchUsesClientAndDocumentLimits() {
        when(clientSearchService.search("balanced", 2)).thenReturn(List.of());
        when(documentSearchService.search("balanced", 3)).thenReturn(List.of());

        var response = restTemplate.getForEntity(
                "/search?q=balanced&client_limit=2&document_limit=3", SearchResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertEquals("balanced", body.query());
        assertThat(body.clients()).isEmpty();
        assertThat(body.documents()).isEmpty();
        assertThat(body.errors()).isEmpty();
        verify(clientSearchService).search("balanced", 2);
        verify(documentSearchService).search("balanced", 3);
    }

}
