package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.rest.dto.ClientResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.DocumentResponse;
import io.gnupinguin.nevis.wealthtech.rest.validation.ClientRequestValidator;
import io.gnupinguin.nevis.wealthtech.service.client.ClientService;
import io.gnupinguin.nevis.wealthtech.service.document.DocumentService;
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
    private final ClientRequestValidator clientRequestValidator;

    @GetMapping("/clients/{clientId}")
    public ClientResponse getClient(@PathVariable UUID clientId) {
        return clientService.getClient(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId));
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@RequestBody CreateClientRequest request) {
        log.info("Create new client request: {}", request);
        clientRequestValidator.validateCreateClient(request);
        return clientService.createClient(request);
    }

    @GetMapping("/clients/{clientId}/documents/{documentId}")
    public DocumentResponse getClientDocument(@PathVariable UUID clientId, @PathVariable UUID documentId) {
        return documentService.getClientDocument(clientId, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
    }

    @PostMapping("/clients/{clientId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createDocument(@PathVariable UUID clientId,
                                           @RequestBody CreateDocumentRequest request) {
        log.info("Create new document request: {}", request);
        clientRequestValidator.validateCreateDocument(request);
        return documentService.createDocument(clientId, request);
    }

}
