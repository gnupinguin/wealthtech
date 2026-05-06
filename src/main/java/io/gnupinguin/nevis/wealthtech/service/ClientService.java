package io.gnupinguin.nevis.wealthtech.service;

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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;

    public ClientService(ClientRepository clientRepository, DocumentRepository documentRepository) {
        this.clientRepository = clientRepository;
        this.documentRepository = documentRepository;
    }

    public Client getClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .map(c -> new Client(c.id(), c.firstName(), c.lastName(), c.email(), c.description(),
                        c.socialLinks().stream().map(SocialLink::value).toList()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId));
    }

    public Client createClient(CreateClientRequest request) {
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

    public Document getClientDocument(UUID clientId, UUID documentId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        return documentRepository.findById(documentId)
                .filter(d -> clientId.equals(d.clientId()))
                .map(d -> new Document(d.id(), d.clientId(), d.title(), d.content(), d.summary(), d.createdAt()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
    }

    public Document createDocument(UUID clientId, CreateDocumentRequest request) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        DocumentEntity saved = documentRepository.save(new DocumentEntity(
                null,
                clientId,
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
