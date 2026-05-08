package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.SocialLink;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchEntity;
import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchService;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchFacadeTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientSearchService clientSearchService;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private Executor searchExecutor;

    @InjectMocks
    private SearchFacade facade;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(searchExecutor).execute(any(Runnable.class));
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

        when(clientSearchService.search("growth", 10)).thenThrow(new IllegalStateException("client search failed"));
        when(documentSearchService.search("growth", 10)).thenReturn(List.of(document));

        var result = facade.search("growth");

        assertThat(result.clients()).isEmpty();
        assertThat(result.documents()).containsExactly(document);
        assertThat(result.errors()).containsExactly("Client results are unavailable");
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

        when(clientSearchService.search("growth", 10)).thenReturn(List.of(new ClientSearchEntity(clientId, 0.76f)));
        when(documentSearchService.search("growth", 10)).thenReturn(List.of(document));
        when(clientRepository.findAllById(List.of(clientId))).thenThrow(new IllegalStateException("clients unavailable"));

        var result = facade.search("growth");

        assertThat(result.clients()).isEmpty();
        assertThat(result.documents()).containsExactly(document);
        assertThat(result.errors()).containsExactly("Client results are unavailable");
    }

    @Test
    void testReturnsClientsWithErrorWhenDocumentSearchFails() {
        var clientId = UUID.randomUUID();
        var client = new ClientEntity(
                clientId,
                "Ada",
                "Lovelace",
                "ada@example.com",
                "Technology investor",
                Instant.EPOCH,
                Set.of(new SocialLink(UUID.randomUUID(), "https://example.com/ada", Instant.EPOCH))
        );

        when(clientSearchService.search("technology", 10)).thenReturn(List.of(new ClientSearchEntity(clientId, 0.92f)));
        when(documentSearchService.search("technology", 10)).thenThrow(new IllegalStateException("document search failed"));
        when(clientRepository.findAllById(List.of(clientId))).thenReturn(List.of(client));

        var result = facade.search("technology");

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
        when(clientSearchService.search("growth", 10)).thenThrow(new IllegalStateException("client search failed"));
        when(documentSearchService.search("growth", 10)).thenThrow(new IllegalStateException("document search failed"));

        var exception = assertThrows(ResponseStatusException.class, () -> facade.search("growth"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
