package io.gnupinguin.nevis.wealthtech.service.document;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentRepository;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.DocumentResponse;
import io.gnupinguin.nevis.wealthtech.service.enrichment.EnrichmentEventService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final EnrichmentEventService enrichmentEventService;

    @Override
    public @NonNull Optional<DocumentResponse> getClientDocument(@NonNull UUID clientId, @NonNull UUID documentId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        return documentRepository.findById(documentId)
                .filter(d -> clientId.equals(d.clientId()))
                .map(d -> new DocumentResponse(d.id(), d.clientId(), d.title(), d.content(), d.summary(), d.createdAt()));
    }

    @Override
    @Transactional
    public @NonNull DocumentResponse createDocument(@NonNull UUID clientId, @NonNull CreateDocumentRequest request) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        var now = Instant.now();
        var saved = documentRepository.save(new DocumentEntity(
                null,
                clientId,
                request.title(),
                request.content(),
                null,
                now,
                now
        ));

        enrichmentEventService.enqueueEvent(saved.id(), JobType.SUMMARY);
        enrichmentEventService.enqueueEvent(saved.id(), JobType.CHUNKING);

        return new DocumentResponse(
                saved.id(),
                saved.clientId(),
                saved.title(),
                saved.content(),
                saved.summary(),
                saved.createdAt()
        );
    }

}
