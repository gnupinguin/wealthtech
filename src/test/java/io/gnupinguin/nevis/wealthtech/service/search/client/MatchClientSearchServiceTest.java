package io.gnupinguin.nevis.wealthtech.service.search.client;

import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchClientSearchServiceTest {

    @Mock
    private ClientRepository repository;

    @Mock
    private QueryNormalizer queryNormalizer;

    @InjectMocks
    private MatchClientSearchService searchService;

    @Test
    void testSearchDelegatesToNormalizerAndRepositoryWithNormalizedQuery() {
        var normalizedQuery = new NormalizedQuery("ada lovelace", "ada lovelace%");
        when(queryNormalizer.normalize("Ada Lovelace")).thenReturn(normalizedQuery);
        when(repository.findMatching("ada lovelace", "ada lovelace%", 5)).thenReturn(List.of());

        var result = searchService.search("Ada Lovelace", 5);

        assertThat(result).isEmpty();
        verify(queryNormalizer).normalize("Ada Lovelace");
        verify(repository).findMatching("ada lovelace", "ada lovelace%", 5);
    }

    @Test
    void testSearchReturnsProjectionsFromRepository() {
        var clientId = UUID.randomUUID();
        var projection = new ClientSearchProjection(clientId, 0.9f);
        var normalizedQuery = new NormalizedQuery("growth", "growth%");
        when(queryNormalizer.normalize("growth")).thenReturn(normalizedQuery);
        when(repository.findMatching("growth", "growth%", 10)).thenReturn(List.of(projection));

        var result = searchService.search("growth", 10);

        assertThat(result).containsExactly(projection);
    }

    @Test
    void testSearchPassesLimitToRepository() {
        var normalizedQuery = new NormalizedQuery("tech", "tech%");
        when(queryNormalizer.normalize("tech")).thenReturn(normalizedQuery);
        when(repository.findMatching("tech", "tech%", 3)).thenReturn(List.of());

        searchService.search("tech", 3);

        verify(repository).findMatching("tech", "tech%", 3);
    }

}
