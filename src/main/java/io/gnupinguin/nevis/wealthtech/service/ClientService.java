package io.gnupinguin.nevis.wealthtech.service;

import io.gnupinguin.nevis.wealthtech.model.*;
import io.gnupinguin.nevis.wealthtech.persistence.*;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.repository.DocumentEnrichmentJobRepository;
import io.gnupinguin.nevis.wealthtech.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final DocumentEnrichmentJobRepository enrichmentJobRepository;

    public Client getClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .map(this::toClient)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId));
    }

    @Transactional
    public Client createClient(CreateClientRequest request) {
        Instant now = Instant.now();
        Set<SocialLink> socialLinks = request.socialLinks() == null
                ? Set.of()
                : request.socialLinks().stream()
                        .map(r -> new SocialLink(null, r.url(), now))
                        .collect(Collectors.toSet());

        ClientEntity saved = clientRepository.save(new ClientEntity(
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

    public Document getClientDocument(UUID clientId, UUID documentId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        return documentRepository.findById(documentId)
                .filter(d -> clientId.equals(d.clientId()))
                .map(d -> new Document(d.id(), d.clientId(), d.title(), d.content(), d.summary(), d.createdAt()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
    }

    @Transactional
    public Document createDocument(UUID clientId, CreateDocumentRequest request) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        Instant now = Instant.now();
        DocumentEntity saved = documentRepository.save(new DocumentEntity(
                null,
                clientId,
                request.title(),
                request.content(),
                null,
                now,
                now
        ));

        publishEnrichmentJobs(saved.id(), now);

        return new Document(
                saved.id(),
                saved.clientId(),
                saved.title(),
                saved.content(),
                saved.summary(),
                saved.createdAt()
        );
    }

    private void publishEnrichmentJobs(UUID documentId, Instant now) {
        enrichmentJobRepository.save(enrichmentJob(documentId, JobType.SUMMARY, now));
        enrichmentJobRepository.save(enrichmentJob(documentId, JobType.CHUNKING, now));
    }

    private static DocumentEnrichmentJobEntity enrichmentJob(UUID documentId, JobType type, Instant now) {
        return new DocumentEnrichmentJobEntity(
                null, documentId, type, JobStatus.PENDING,
                0, 3, null, now, null, null, now, now
        );
    }

    private Client toClient(ClientEntity c) {
        List<SocialLinkDto> socialLinks = c.socialLinks().stream()
                .map(s -> new SocialLinkDto(s.id(), s.url(), s.createdAt()))
                .toList();
        return new Client(c.id(), c.firstName(), c.lastName(), c.email(), c.description(), c.createdAt(), socialLinks);
    }

}
