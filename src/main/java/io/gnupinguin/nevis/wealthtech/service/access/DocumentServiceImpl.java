package io.gnupinguin.nevis.wealthtech.service.access;

import io.gnupinguin.nevis.wealthtech.model.Document;
import io.gnupinguin.nevis.wealthtech.persistence.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import io.gnupinguin.nevis.wealthtech.repository.ClientRepository;
import io.gnupinguin.nevis.wealthtech.repository.DocumentRepository;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.service.enrichment.JobService;
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
    private final JobService jobService;

    @Override
    public @NonNull Optional<Document> getClientDocument(@NonNull UUID clientId, @NonNull UUID documentId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found: " + clientId);
        }
        return documentRepository.findById(documentId)
                .filter(d -> clientId.equals(d.clientId()))
                .map(d -> new Document(d.id(), d.clientId(), d.title(), d.content(), d.summary(), d.createdAt()));
    }

    @Override
    @Transactional
    public @NonNull Document createDocument(@NonNull UUID clientId, @NonNull CreateDocumentRequest request) {
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

        jobService.publishJob(saved.id(), JobType.SUMMARY);
        jobService.publishJob(saved.id(), JobType.CHUNKING);

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
