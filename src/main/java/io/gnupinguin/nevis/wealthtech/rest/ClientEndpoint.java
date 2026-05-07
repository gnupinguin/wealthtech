package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.model.Client;
import io.gnupinguin.nevis.wealthtech.model.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.model.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.model.Document;
import io.gnupinguin.nevis.wealthtech.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
public class ClientEndpoint {

    private final ClientService clientService;

    public ClientEndpoint(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/clients/{clientId}")
    public Client getClient(@PathVariable UUID clientId) {
        return clientService.getClient(clientId);
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public Client createClient(@RequestBody CreateClientRequest request) {
        log.info("Create new client request: {}", request);
        return clientService.createClient(request);
    }

    @GetMapping("/clients/{clientId}/documents/{documentId}")
    public Document getClientDocument(@PathVariable UUID clientId, @PathVariable UUID documentId) {
        return clientService.getClientDocument(clientId, documentId);
    }

    @PostMapping("/clients/{clientId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@PathVariable UUID clientId,
                                   @RequestBody CreateDocumentRequest request) {
        log.info("Create new document request: {}", request);
        return clientService.createDocument(clientId, request);
    }

}
