package io.gnupinguin.nevis.wealthtech.service.search.client;

import io.gnupinguin.nevis.wealthtech.persistence.entity.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.SocialLinkEntity;
import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultClientSearchResultHydratorTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private DefaultClientSearchResultHydrator hydrator;

    @Test
    void testReturnsEmptyListWhenThereAreNoSearchResults() {
        var result = hydrator.hydrate(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(clientRepository);
    }

    @Test
    void testHydratesClientSearchResultsInSearchResultOrder() {
        var firstClientId = UUID.randomUUID();
        var secondClientId = UUID.randomUUID();
        var firstProjection = new ClientSearchProjection(firstClientId, 0.91f);
        var secondProjection = new ClientSearchProjection(secondClientId, 0.82f);
        var firstClient = client(firstClientId, "Ada", "Lovelace", "ada@example.com", "Technology investor");
        var secondClient = client(secondClientId, "Grace", "Hopper", "grace@example.com", "Income investor");

        when(clientRepository.findAllById(List.of(firstClientId, secondClientId)))
                .thenReturn(List.of(secondClient, firstClient));

        var result = hydrator.hydrate(List.of(firstProjection, secondProjection));

        assertThat(result).extracting(ClientSearchResult::id)
                .containsExactly(firstClientId, secondClientId);
        assertThat(result).extracting(ClientSearchResult::score)
                .containsExactly(0.91f, 0.82f);
        assertThat(result).first()
                .satisfies(client -> {
                    assertThat(client.firstName()).isEqualTo("Ada");
                    assertThat(client.lastName()).isEqualTo("Lovelace");
                    assertThat(client.email()).isEqualTo("ada@example.com");
                    assertThat(client.description()).isEqualTo("Technology investor");
                    assertThat(client.socialLinks()).containsExactly("https://example.com/ada");
                });
    }

    @Test
    void testSkipsMissingClients() {
        var existingClientId = UUID.randomUUID();
        var missingClientId = UUID.randomUUID();
        var existingProjection = new ClientSearchProjection(existingClientId, 0.91f);
        var missingProjection = new ClientSearchProjection(missingClientId, 0.82f);

        when(clientRepository.findAllById(List.of(existingClientId, missingClientId)))
                .thenReturn(List.of(client(existingClientId, "Ada", "Lovelace", "ada@example.com", "Technology investor")));

        var result = hydrator.hydrate(List.of(existingProjection, missingProjection));

        assertThat(result).singleElement()
                .satisfies(client -> {
                    assertThat(client.id()).isEqualTo(existingClientId);
                    assertThat(client.score()).isEqualTo(0.91f);
                });
    }

    private static ClientEntity client(UUID id,
                                       String firstName,
                                       String lastName,
                                       String email,
                                       String description) {
        return new ClientEntity(
                id,
                firstName,
                lastName,
                email,
                description,
                Instant.EPOCH,
                Set.of(new SocialLinkEntity(UUID.randomUUID(), "https://example.com/" + firstName.toLowerCase(), Instant.EPOCH))
        );
    }

}
