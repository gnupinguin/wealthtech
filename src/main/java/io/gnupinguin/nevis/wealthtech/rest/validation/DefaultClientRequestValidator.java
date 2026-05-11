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

    private static final int CLIENT_NAME_MAX_LENGTH = 100;
    private static final int CLIENT_EMAIL_MAX_LENGTH = 320;
    private static final int CLIENT_DESCRIPTION_MAX_LENGTH = 4096;
    private static final int SOCIAL_LINK_URL_MAX_LENGTH = 2048;
    private static final int DOCUMENT_TITLE_MAX_LENGTH = 255;
    private static final int DOCUMENT_CONTENT_MAX_LENGTH = 1_000_000;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public void validateCreateClient(@Nullable CreateClientRequest request) {
        if (request == null) {
            throwBadRequest("Request body is required");
        }
        validateRequired("first_name", request.firstName());
        validateRequired("last_name", request.lastName());
        validateRequired("email", request.email());
        validateMaxLength("first_name", request.firstName(), CLIENT_NAME_MAX_LENGTH);
        validateMaxLength("last_name", request.lastName(), CLIENT_NAME_MAX_LENGTH);
        validateMaxLength("email", request.email(), CLIENT_EMAIL_MAX_LENGTH);
        validateMaxLength("description", request.description(), CLIENT_DESCRIPTION_MAX_LENGTH);
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
        validateMaxLength("title", request.title(), DOCUMENT_TITLE_MAX_LENGTH);
        validateMaxLength("content", request.content(), DOCUMENT_CONTENT_MAX_LENGTH);
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
            validateMaxLength("social_links entries", socialLink, SOCIAL_LINK_URL_MAX_LENGTH);
        }
    }

    private static void validateRequired(@NonNull String fieldName, @Nullable String value) {
        if (isBlank(value)) {
            throwBadRequest(fieldName + " is required");
        }
    }

    private static void validateMaxLength(@NonNull String fieldName, @Nullable String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throwBadRequest(fieldName + " must be at most " + maxLength + " characters");
        }
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static void throwBadRequest(@NonNull String reason) {
        throw new BadRequestException(reason);
    }

}
