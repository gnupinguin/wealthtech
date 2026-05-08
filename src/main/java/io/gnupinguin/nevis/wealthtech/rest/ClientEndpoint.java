package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.model.Client;
import io.gnupinguin.nevis.wealthtech.model.Document;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.service.access.ClientService;
import io.gnupinguin.nevis.wealthtech.service.access.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ClientEndpoint {

    private final ClientService clientService;
    private final DocumentService documentService;

    @GetMapping("/clients/{clientId}")
    public Client getClient(@PathVariable UUID clientId) {
        return clientService.getClient(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId));
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public Client createClient(@RequestBody CreateClientRequest request) {
        log.info("Create new client request: {}", request);
        return clientService.createClient(request);
    }

    @GetMapping("/clients/{clientId}/documents/{documentId}")
    public Document getClientDocument(@PathVariable UUID clientId, @PathVariable UUID documentId) {
        return documentService.getClientDocument(clientId, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
    }

    @PostMapping("/clients/{clientId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@PathVariable UUID clientId,
                                   @RequestBody CreateDocumentRequest request) {
        log.info("Create new document request: {}", request);
        return documentService.createDocument(clientId, request);
    }

}
