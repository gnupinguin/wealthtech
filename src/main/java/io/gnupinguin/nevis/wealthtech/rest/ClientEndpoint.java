package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.model.Client;
import io.gnupinguin.nevis.wealthtech.model.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.model.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.model.Document;
import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.SocialLink;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class ClientEndpoint {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;

    public ClientEndpoint(ClientRepository clientRepository, DocumentRepository documentRepository) {
        this.clientRepository = clientRepository;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public Client createClient(@RequestBody CreateClientRequest request) {
        List<SocialLink> socialLinks = request.socialLinks() == null
                ? List.of()
                : request.socialLinks().stream().map(SocialLink::new).toList();

        ClientEntity saved = clientRepository.save(new ClientEntity(
                null,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.description(),
                socialLinks
        ));

        return new Client(
                saved.id(),
                saved.firstName(),
                saved.lastName(),
                saved.email(),
                saved.description(),
                saved.socialLinks().stream().map(SocialLink::value).toList()
        );
    }

    @PostMapping("/clients/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@PathVariable UUID id,
                                   @RequestBody CreateDocumentRequest request) {
        DocumentEntity saved = documentRepository.save(new DocumentEntity(
                null,
                id,
                request.title(),
                request.content(),
                null,
                Instant.now()
        ));

        return new Document(
                saved.id(),
                saved.clientId(),
                saved.title(),
                saved.content(),
                saved.summary(),
                saved.createdAt()
        );
    }
}
