package io.gnupinguin.nevis.wealthtech.service.client;

import io.gnupinguin.nevis.wealthtech.persistence.entity.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.SocialLinkEntity;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.rest.dto.ClientResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    public @NonNull Optional<ClientResponse> getClient(@NonNull UUID clientId) {
        return clientRepository.findById(clientId)
                .map(this::toClient);
    }

    @Override
    @Transactional
    public @NonNull ClientResponse createClient(@NonNull CreateClientRequest request) {
        var now = Instant.now();
        var socialLinks = getSocialLinks(request, now);

        var saved = clientRepository.save(new ClientEntity(
                null,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.description(),
                now,
                socialLinks
        ));

        return toClient(saved);
    }

    private static @NonNull Set<SocialLinkEntity> getSocialLinks(@NonNull CreateClientRequest request, Instant now) {
        return request.socialLinks() == null ? Set.of() : mapLinks(request, now);
    }

    private static @NonNull Set<SocialLinkEntity> mapLinks(@NonNull CreateClientRequest request, Instant now) {
        return request.socialLinks().stream()
                .map(url -> new SocialLinkEntity(null, url, now))
                .collect(Collectors.toSet());
    }

    private ClientResponse toClient(@NonNull ClientEntity client) {
        var socialLinks = client.socialLinks().stream()
                .map(SocialLinkEntity::url)
                .toList();
        return new ClientResponse(client.id(), client.firstName(), client.lastName(), client.email(), client.description(), client.createdAt(), socialLinks);
    }

}
