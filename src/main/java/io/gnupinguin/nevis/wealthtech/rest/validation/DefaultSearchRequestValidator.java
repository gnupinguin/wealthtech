package io.gnupinguin.nevis.wealthtech.rest.validation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultSearchRequestValidator implements SearchRequestValidator {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_QUERY_LENGTH = 127;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_CLIENT_LIMIT = 20;
    private static final int MAX_DOCUMENT_LIMIT = 50;

    @Override
    public void validate(@Nullable String query, int clientLimit, int documentLimit) {
        validateQuery(query);
        validateLimit("clientLimit", clientLimit, MAX_CLIENT_LIMIT);
        validateLimit("documentLimit", documentLimit, MAX_DOCUMENT_LIMIT);
    }

    private static void validateQuery(@Nullable String query) {
        var queryLength = query == null ? 0 : query.trim().length();
        if (queryLength < MIN_QUERY_LENGTH || queryLength > MAX_QUERY_LENGTH) {
            throwBadRequest("q must be between %d and %d characters".formatted(MIN_QUERY_LENGTH, MAX_QUERY_LENGTH));
        }
    }

    private static void validateLimit(@NonNull String parameterName, int limit, int maxLimit) {
        if (limit < MIN_LIMIT || limit > maxLimit) {
            throwBadRequest("%s must be between %d and %d".formatted(parameterName, DefaultSearchRequestValidator.MIN_LIMIT, maxLimit));
        }
    }

    private static void throwBadRequest(@NonNull String reason) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

}
