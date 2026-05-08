package io.gnupinguin.nevis.wealthtech.service.document;

import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.DocumentResponse;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface DocumentService {

    @NonNull
    DocumentResponse createDocument(@NonNull UUID clientId, @NonNull CreateDocumentRequest request);

    @NonNull
    Optional<DocumentResponse> getClientDocument(@NonNull UUID clientId, @NonNull UUID documentId);

}
