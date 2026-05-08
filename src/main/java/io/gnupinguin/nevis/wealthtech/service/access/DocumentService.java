package io.gnupinguin.nevis.wealthtech.service.access;

import io.gnupinguin.nevis.wealthtech.model.Document;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateDocumentRequest;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface DocumentService {

    @NonNull
    Document createDocument(@NonNull UUID clientId, @NonNull CreateDocumentRequest request);

    @NonNull
    Optional<Document> getClientDocument(@NonNull UUID clientId, @NonNull UUID documentId);

}
