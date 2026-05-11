package io.gnupinguin.nevis.wealthtech.rest.validation;

import io.gnupinguin.nevis.wealthtech.exception.BadRequestException;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class DefaultClientRequestValidator implements ClientRequestValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public void validateCreateClient(@Nullable CreateClientRequest request) {
        if (request == null) {
            throwBadRequest("Request body is required");
        }
        validateRequired("first_name", request.firstName());
        validateRequired("last_name", request.lastName());
        validateRequired("email", request.email());
        validateEmail(request.email());
        validateSocialLinks(request);
    }

    @Override
    public void validateCreateDocument(@Nullable CreateDocumentRequest request) {
        if (request == null) {
            throwBadRequest("Request body is required");
        }
        validateRequired("title", request.title());
        validateRequired("content", request.content());
    }

    private static void validateEmail(@NonNull String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throwBadRequest("email must be a valid email address");
        }
    }

    private static void validateSocialLinks(@NonNull CreateClientRequest request) {
        if (request.socialLinks() == null) {
            return;
        }
        for (var socialLink : request.socialLinks()) {
            if (isBlank(socialLink)) {
                throwBadRequest("social_links entries must be non-blank");
            }
        }
    }

    private static void validateRequired(@NonNull String fieldName, @Nullable String value) {
        if (isBlank(value)) {
            throwBadRequest(fieldName + " is required");
        }
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static void throwBadRequest(@NonNull String reason) {
        throw new BadRequestException(reason);
    }

}
