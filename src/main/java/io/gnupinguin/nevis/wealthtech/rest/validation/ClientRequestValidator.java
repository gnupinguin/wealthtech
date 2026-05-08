package io.gnupinguin.nevis.wealthtech.rest.validation;

import io.gnupinguin.nevis.wealthtech.rest.model.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateDocumentRequest;
import org.jspecify.annotations.Nullable;

public interface ClientRequestValidator {

    void validateCreateClient(@Nullable CreateClientRequest request);

    void validateCreateDocument(@Nullable CreateDocumentRequest request);

}
