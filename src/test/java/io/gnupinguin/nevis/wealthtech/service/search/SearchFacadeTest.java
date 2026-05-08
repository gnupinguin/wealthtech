package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.config.SearchProperties;
import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResultHydrator;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchService;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchFacadeTest {

    @Mock
    private ClientSearchService clientSearchService;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private ClientSearchResultHydrator clientSearchResultHydrator;

    @Mock
    private Executor searchExecutor;

    private SearchFacade facade;

    @BeforeEach
    void setUp() {
        var searchProperties = new SearchProperties(4, 10_000, 30_000);
        facade = new SearchFacade(
                clientSearchService,
                documentSearchService,
                clientSearchResultHydrator,
                searchProperties,
                searchExecutor
        );
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(searchExecutor).execute(any(Runnable.class));
    }

    @Test
    void testUsesClientAndDocumentLimits() {
        when(clientSearchService.search("growth", 2)).thenReturn(List.of());
        when(documentSearchService.search("growth", 3)).thenReturn(List.of());

        var result = facade.search("growth", 2, 3);

        assertThat(result.clients()).isEmpty();
        assertThat(result.documents()).isEmpty();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void testReturnsClientsAndDocumentsWhenBothSearchesSucceed() {
        var clientId = UUID.randomUUID();
        var clientSearchProjection = new ClientSearchProjection(clientId, 0.91f);
        var client = new ClientSearchResult(
                clientId,
                "Ada",
                "Lovelace",
                "ada@example.com",
                "Technology investor",
                List.of("https://example.com/ada"),
                0.91f
        );
        var document = new DocumentSearchResult(
                UUID.randomUUID(),
                clientId,
                "Portfolio Summary",
                "Growth allocation",
                "A portfolio summary",
                0.87f
        );

        when(clientSearchService.search("growth", 5)).thenReturn(List.of(clientSearchProjection));
        when(documentSearchService.search("growth", 10)).thenReturn(List.of(document));
        when(clientSearchResultHydrator.hydrate(List.of(clientSearchProjection))).thenReturn(List.of(client));

        var result = facade.search("growth", 5, 10);

        assertThat(result.clients()).containsExactly(client);
        assertThat(result.documents()).containsExactly(document);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void testReturnsDocumentsWithErrorWhenClientSearchFails() {
        var document = new DocumentSearchResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Portfolio Summary",
                "Growth allocation",
                "A portfolio summary",
                0.87f
        );

        when(clientSearchService.search("growth", 5)).thenThrow(new IllegalStateException("client search failed"));
        when(documentSearchService.search("growth", 10)).thenReturn(List.of(document));

        var result = facade.search("growth", 5, 10);

        assertThat(result.clients()).isEmpty();
        assertThat(result.documents()).containsExactly(document);
        assertThat(result.errors()).containsExactly("Client results are unavailable");
        verifyNoInteractions(clientSearchResultHydrator);
    }

    @Test
    void testReturnsDocumentsWithErrorWhenClientHydrationFails() {
        var clientId = UUID.randomUUID();
        var document = new DocumentSearchResult(
                UUID.randomUUID(),
                clientId,
                "Portfolio Summary",
                "Growth allocation",
                "A portfolio summary",
                0.87f
        );

        var clientSearchProjection = new ClientSearchProjection(clientId, 0.76f);

        when(clientSearchService.search("growth", 5)).thenReturn(List.of(clientSearchProjection));
        when(documentSearchService.search("growth", 10)).thenReturn(List.of(document));
        when(clientSearchResultHydrator.hydrate(List.of(clientSearchProjection)))
                .thenThrow(new IllegalStateException("clients unavailable"));

        var result = facade.search("growth", 5, 10);

        assertThat(result.clients()).isEmpty();
        assertThat(result.documents()).containsExactly(document);
        assertThat(result.errors()).containsExactly("Client results are unavailable");
    }

    @Test
    void testReturnsClientsWithErrorWhenDocumentSearchFails() {
        var clientId = UUID.randomUUID();
        var clientSearchProjection = new ClientSearchProjection(clientId, 0.92f);
        var client = new ClientSearchResult(
                clientId,
                "Ada",
                "Lovelace",
                "ada@example.com",
                "Technology investor",
                List.of("https://example.com/ada"),
                0.92f
        );

        when(clientSearchService.search("technology", 5)).thenReturn(List.of(clientSearchProjection));
        when(documentSearchService.search("technology", 10)).thenThrow(new IllegalStateException("document search failed"));
        when(clientSearchResultHydrator.hydrate(List.of(clientSearchProjection))).thenReturn(List.of(client));

        var result = facade.search("technology", 5, 10);

        assertThat(result.clients()).singleElement()
                .satisfies(searchResult -> {
                    assertThat(searchResult.id()).isEqualTo(clientId);
                    assertThat(searchResult.firstName()).isEqualTo("Ada");
                    assertThat(searchResult.socialLinks()).containsExactly("https://example.com/ada");
                });
        assertThat(result.documents()).isEmpty();
        assertThat(result.errors()).containsExactly("Document results are unavailable");
    }

    @Test
    void testThrowsServiceUnavailableWhenAllSearchesFail() {
        when(clientSearchService.search("growth", 5)).thenThrow(new IllegalStateException("client search failed"));
        when(documentSearchService.search("growth", 10)).thenThrow(new IllegalStateException("document search failed"));

        var exception = assertThrows(ResponseStatusException.class, () -> facade.search("growth", 5, 10));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void testThrowsServiceUnavailableWhenDocumentSearchAndClientHydrationFail() {
        var clientId = UUID.randomUUID();
        var clientSearchProjection = new ClientSearchProjection(clientId, 0.76f);

        when(clientSearchService.search("growth", 5)).thenReturn(List.of(clientSearchProjection));
        when(documentSearchService.search("growth", 10)).thenThrow(new IllegalStateException("document search failed"));
        when(clientSearchResultHydrator.hydrate(List.of(clientSearchProjection)))
                .thenThrow(new IllegalStateException("clients unavailable"));

        var exception = assertThrows(ResponseStatusException.class, () -> facade.search("growth", 5, 10));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void testUsesConfiguredTimeouts() {
        var searchProperties = new SearchProperties(4, 1, 1);
        facade = new SearchFacade(
                clientSearchService,
                documentSearchService,
                clientSearchResultHydrator,
                searchProperties,
                command -> {
                }
        );

        var exception = assertThrows(ResponseStatusException.class, () -> facade.search("growth", 5, 10));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
