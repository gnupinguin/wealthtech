package integration;

import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchService;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

class SearchEndpointValidationIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ClientSearchService clientSearchService;

    @MockitoBean
    private DocumentSearchService documentSearchService;

    @Test
    void testSearchRejectsShortQuery() {
        var response = restTemplate.getForEntity("/search?q=ab", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("q must be between 3 and 127 characters", response.getBody());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

    @Test
    void testSearchRejectsLongQuery() {
        var response = restTemplate.getForEntity("/search?q=" + "a".repeat(128), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("q must be between 3 and 127 characters", response.getBody());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

    @Test
    void testSearchRejectsBlankQuery() {
        var response = restTemplate.getForEntity("/search?q=   ", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("q must be between 3 and 127 characters", response.getBody());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

    @Test
    void testSearchRejectsMissingQuery() {
        var response = restTemplate.getForEntity("/search", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

    @Test
    void testSearchRejectsClientLimitAboveMaximum() {
        var response = restTemplate.getForEntity(
                "/search?q=balanced&client_limit=21", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("client_limit must be between 1 and 20", response.getBody());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

    @Test
    void testSearchRejectsClientLimitBelowMinimum() {
        var response = restTemplate.getForEntity(
                "/search?q=balanced&client_limit=0", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("client_limit must be between 1 and 20", response.getBody());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

    @Test
    void testSearchRejectsDocumentLimitAboveMaximum() {
        var response = restTemplate.getForEntity(
                "/search?q=balanced&document_limit=51", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("document_limit must be between 1 and 50", response.getBody());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

    @Test
    void testSearchRejectsDocumentLimitBelowMinimum() {
        var response = restTemplate.getForEntity(
                "/search?q=balanced&document_limit=0", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("document_limit must be between 1 and 50", response.getBody());
        verifyNoInteractions(clientSearchService, documentSearchService);
    }

}
