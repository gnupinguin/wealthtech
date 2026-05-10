package io.gnupinguin.nevis.wealthtech.service.document;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentRepository;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.service.enrichment.EnrichmentEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EnrichmentEventService enrichmentEventService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    void testGetClientDocumentThrowsNotFoundWhenClientDoesNotExist() {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(false);

        var exception = assertThrows(ResponseStatusException.class,
                () -> documentService.getClientDocument(clientId, documentId));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetClientDocumentReturnsEmptyWhenDocumentNotFound() {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        var result = documentService.getClientDocument(clientId, documentId);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetClientDocumentReturnsEmptyWhenDocumentBelongsToDifferentClient() {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        var otherClientId = UUID.randomUUID();
        var entity = new DocumentEntity(documentId, otherClientId, "Title", "Content", null, Instant.EPOCH, Instant.EPOCH);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(entity));

        var result = documentService.getClientDocument(clientId, documentId);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetClientDocumentReturnsMappedResponseWhenDocumentBelongsToClient() {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        var entity = new DocumentEntity(documentId, clientId, "My Title", "My Content", "Summary", Instant.EPOCH, Instant.EPOCH);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(entity));

        var result = documentService.getClientDocument(clientId, documentId);

        assertThat(result).isPresent();
        result.ifPresent(doc -> {
            assertThat(doc.id()).isEqualTo(documentId);
            assertThat(doc.clientId()).isEqualTo(clientId);
            assertThat(doc.title()).isEqualTo("My Title");
            assertThat(doc.content()).isEqualTo("My Content");
            assertThat(doc.summary()).isEqualTo("Summary");
        });
    }

    @Test
    void testCreateDocumentThrowsNotFoundWhenClientDoesNotExist() {
        var clientId = UUID.randomUUID();
        var request = new CreateDocumentRequest("Title", "Content");
        when(clientRepository.existsById(clientId)).thenReturn(false);

        var exception = assertThrows(ResponseStatusException.class,
                () -> documentService.createDocument(clientId, request));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(documentRepository, enrichmentEventService);
    }

    @Test
    void testCreateDocumentSavesDocumentAndPublishesBothJobs() {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        var request = new CreateDocumentRequest("Title", "Content");
        var saved = new DocumentEntity(documentId, clientId, "Title", "Content", null, Instant.EPOCH, Instant.EPOCH);
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(documentRepository.save(any())).thenReturn(saved);

        var result = documentService.createDocument(clientId, request);

        verify(documentRepository).save(any());
        verify(enrichmentEventService).enqueueEvent(documentId, JobType.SUMMARY);
        verify(enrichmentEventService).enqueueEvent(documentId, JobType.CHUNKING);

        assertThat(result.id()).isEqualTo(documentId);
        assertThat(result.clientId()).isEqualTo(clientId);
        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.summary()).isNull();
    }

    @Test
    void testCreateDocumentIsTransactionalSoDocumentAndOutboxEventsCommitTogether() throws NoSuchMethodException {
        Method method = DocumentServiceImpl.class.getMethod("createDocument", UUID.class, CreateDocumentRequest.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

}
