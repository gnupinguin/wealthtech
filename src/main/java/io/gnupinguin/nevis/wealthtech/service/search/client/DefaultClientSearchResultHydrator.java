package io.gnupinguin.nevis.wealthtech.service.search.client;

import io.gnupinguin.nevis.wealthtech.persistence.entity.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.SocialLinkEntity;
import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultClientSearchResultHydrator implements ClientSearchResultHydrator {

    private final ClientRepository clientRepository;

    @Override
    public @NonNull List<ClientSearchResult> hydrate(@NonNull List<ClientSearchProjection> searchResults) {
        if (searchResults.isEmpty()) {
            return List.of();
        }

        var clientIds = searchResults.stream().map(ClientSearchProjection::id).toList();
        var clientEntities = clientRepository.findAllById(clientIds);
        var clientsById = StreamSupport.stream(clientEntities.spliterator(), false)
                .collect(Collectors.toMap(ClientEntity::id, client -> client));

        var clients = new ArrayList<ClientSearchResult>();
        for (var result : searchResults) {
            var client = clientsById.get(result.id());
            if (client == null) {
                log.warn("Client search returned missing client {}", result.id());
            } else {
                clients.add(toClientSearchResult(client, result.score()));
            }
        }
        return clients;
    }

    private static @NonNull ClientSearchResult toClientSearchResult(@NonNull ClientEntity client, float score) {
        var socialLinks = client.socialLinks().stream()
                .map(SocialLinkEntity::url)
                .toList();

        return new ClientSearchResult(
                client.id(),
                client.firstName(),
                client.lastName(),
                client.email(),
                client.description(),
                socialLinks,
                score
        );
    }

}
