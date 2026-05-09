package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.exception.BadRequestException;
import io.gnupinguin.nevis.wealthtech.rest.dto.ClientResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.DocumentResponse;
import io.gnupinguin.nevis.wealthtech.rest.validation.ClientRequestValidator;
import io.gnupinguin.nevis.wealthtech.service.client.ClientService;
import io.gnupinguin.nevis.wealthtech.service.document.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientEndpointTest {

    @Mock
    private ClientService clientService;

    @Mock
    private DocumentService documentService;

    @Mock
    private ClientRequestValidator clientRequestValidator;

    @InjectMocks
    private ClientEndpoint clientEndpoint;

    @Test
    void testGetClientReturnsClientWhenFound() {
        var clientId = UUID.randomUUID();
        var expected = new ClientResponse(clientId, "Ada", "Lovelace", "ada@example.com", null, Instant.now(), List.of());
        when(clientService.getClient(clientId)).thenReturn(Optional.of(expected));

        var actual = clientEndpoint.getClient(clientId);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void testGetClientThrowsNotFoundWhenClientAbsent() {
        var clientId = UUID.randomUUID();
        when(clientService.getClient(clientId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class, () -> clientEndpoint.getClient(clientId));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
    }

    // --- createClient ---

    @Test
    void testCreateClientDelegatesToValidatorAndService() {
        var request = new CreateClientRequest("Ada", "Lovelace", "ada@example.com", null, null);
        var expected = new ClientResponse(UUID.randomUUID(), "Ada", "Lovelace", "ada@example.com", null, Instant.now(), List.of());
        when(clientService.createClient(request)).thenReturn(expected);

        var actual = clientEndpoint.createClient(request);

        verify(clientRequestValidator).validateCreateClient(request);
        verify(clientService).createClient(request);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void testCreateClientPropagatesValidationException() {
        var request = new CreateClientRequest(null, "Lovelace", "ada@example.com", null, null);
        doThrow(new BadRequestException("first_name is required")).when(clientRequestValidator).validateCreateClient(request);

        assertThrows(BadRequestException.class, () -> clientEndpoint.createClient(request));
        verifyNoInteractions(clientService);
    }

    @Test
    void testGetClientDocumentReturnsDocumentWhenFound() {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        var expected = new DocumentResponse(documentId, clientId, "Portfolio Q1", "content", "summary", Instant.now());
        when(documentService.getClientDocument(clientId, documentId)).thenReturn(Optional.of(expected));

        var actual = clientEndpoint.getClientDocument(clientId, documentId);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void testGetClientDocumentThrowsNotFoundWhenDocumentAbsent() {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        when(documentService.getClientDocument(clientId, documentId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class, () -> clientEndpoint.getClientDocument(clientId, documentId));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void testCreateDocumentDelegatesToValidatorAndService() {
        var clientId = UUID.randomUUID();
        var request = new CreateDocumentRequest("Investment Policy", "Policy content");
        var expected = new DocumentResponse(UUID.randomUUID(), clientId, "Investment Policy", "Policy content", "summary", Instant.now());
        when(documentService.createDocument(clientId, request)).thenReturn(expected);

        var actual = clientEndpoint.createDocument(clientId, request);

        verify(clientRequestValidator).validateCreateDocument(request);
        verify(documentService).createDocument(clientId, request);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void testCreateDocumentPropagatesValidationException() {
        var clientId = UUID.randomUUID();
        var request = new CreateDocumentRequest(null, "content");
        doThrow(new BadRequestException("title is required")).when(clientRequestValidator).validateCreateDocument(request);

        assertThrows(BadRequestException.class, () -> clientEndpoint.createDocument(clientId, request));
        verifyNoInteractions(documentService);
    }

}
