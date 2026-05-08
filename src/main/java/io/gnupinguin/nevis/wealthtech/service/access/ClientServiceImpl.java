package io.gnupinguin.nevis.wealthtech.service.access;

import io.gnupinguin.nevis.wealthtech.model.Client;
import io.gnupinguin.nevis.wealthtech.model.SocialLinkDto;
import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.SocialLink;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateClientRequest;
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
    public @NonNull Optional<Client> getClient(@NonNull UUID clientId) {
        return clientRepository.findById(clientId)
                .map(this::toClient);
    }

    @Override
    @Transactional
    public @NonNull Client createClient(@NonNull CreateClientRequest request) {
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

    private static @NonNull Set<SocialLink> getSocialLinks(@NonNull CreateClientRequest request, Instant now) {
        return request.socialLinks() == null ? Set.of() : mapLinks(request, now);
    }

    private static @NonNull Set<SocialLink> mapLinks(@NonNull CreateClientRequest request, Instant now) {
        return request.socialLinks().stream()
                .map(r -> new SocialLink(null, r.url(), now))
                .collect(Collectors.toSet());
    }

    private Client toClient(@NonNull ClientEntity client) {
        var socialLinks = client.socialLinks().stream()
                .map(link -> new SocialLinkDto(link.id(), link.url(), link.createdAt()))
                .toList();
        return new Client(client.id(), client.firstName(), client.lastName(), client.email(), client.description(), client.createdAt(), socialLinks);
    }

}
