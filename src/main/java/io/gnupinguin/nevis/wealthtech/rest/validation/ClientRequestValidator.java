package io.gnupinguin.nevis.wealthtech.rest.validation;

import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import org.jspecify.annotations.Nullable;

public interface ClientRequestValidator {

    void validateCreateClient(@Nullable CreateClientRequest request);

    void validateCreateDocument(@Nullable CreateDocumentRequest request);

}
