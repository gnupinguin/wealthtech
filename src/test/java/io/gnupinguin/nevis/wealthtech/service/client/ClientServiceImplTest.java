package io.gnupinguin.nevis.wealthtech.service.client;

import io.gnupinguin.nevis.wealthtech.persistence.entity.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.SocialLinkEntity;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    void testGetClientReturnsEmptyWhenClientNotFound() {
        var id = UUID.randomUUID();
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        var result = clientService.getClient(id);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetClientReturnsMappedResponseWhenClientExists() {
        var id = UUID.randomUUID();
        var now = Instant.EPOCH;
        var entity = new ClientEntity(id, "Ada", "Lovelace", "ada@example.com", "Investor", now,
                Set.of(new SocialLinkEntity(UUID.randomUUID(), "https://example.com/ada", now)));
        when(clientRepository.findById(id)).thenReturn(Optional.of(entity));

        var result = clientService.getClient(id);

        assertThat(result).isPresent();
        result.ifPresent(client -> {
            assertThat(client.id()).isEqualTo(id);
            assertThat(client.firstName()).isEqualTo("Ada");
            assertThat(client.lastName()).isEqualTo("Lovelace");
            assertThat(client.email()).isEqualTo("ada@example.com");
            assertThat(client.description()).isEqualTo("Investor");
            assertThat(client.createdAt()).isEqualTo(now);
            assertThat(client.socialLinks()).containsExactly("https://example.com/ada");
        });
    }

    @Test
    void testCreateClientSavesEntityWithNullIdAndReturnsResponse() {
        var request = new CreateClientRequest("Ada", "Lovelace", "ada@example.com", "Investor", null);
        var savedId = UUID.randomUUID();
        var savedEntity = new ClientEntity(savedId, "Ada", "Lovelace", "ada@example.com", "Investor", Instant.EPOCH, Set.of());
        when(clientRepository.save(any())).thenReturn(savedEntity);

        var result = clientService.createClient(request);

        var captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().id()).isNull();
        assertThat(captor.getValue().firstName()).isEqualTo("Ada");
        assertThat(captor.getValue().socialLinks()).isEmpty();

        assertThat(result.id()).isEqualTo(savedId);
        assertThat(result.firstName()).isEqualTo("Ada");
    }

    @Test
    void testCreateClientWithNullSocialLinksSavesEmptySet() {
        var request = new CreateClientRequest("Ada", "Lovelace", "ada@example.com", "Investor", null);
        var savedEntity = new ClientEntity(UUID.randomUUID(), "Ada", "Lovelace", "ada@example.com", "Investor", Instant.EPOCH, Set.of());
        when(clientRepository.save(any())).thenReturn(savedEntity);

        clientService.createClient(request);

        var captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().socialLinks()).isEmpty();
    }

    @Test
    void testCreateClientWithSocialLinksMapsLinksWithNullId() {
        var request = new CreateClientRequest("Ada", "Lovelace", "ada@example.com", "Investor",
                List.of("https://linkedin.com/ada"));
        var linkId = UUID.randomUUID();
        var savedEntity = new ClientEntity(UUID.randomUUID(), "Ada", "Lovelace", "ada@example.com", "Investor",
                Instant.EPOCH, Set.of(new SocialLinkEntity(linkId, "https://linkedin.com/ada", Instant.EPOCH)));
        when(clientRepository.save(any())).thenReturn(savedEntity);

        var result = clientService.createClient(request);

        var captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().socialLinks()).singleElement()
                .satisfies(link -> {
                    assertThat(link.id()).isNull();
                    assertThat(link.url()).isEqualTo("https://linkedin.com/ada");
                });
        assertThat(result.socialLinks()).containsExactly("https://linkedin.com/ada");
    }

}
