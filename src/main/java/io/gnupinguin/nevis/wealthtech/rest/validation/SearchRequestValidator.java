package io.gnupinguin.nevis.wealthtech.rest.validation;

import org.jspecify.annotations.Nullable;

public interface SearchRequestValidator {

    void validate(@Nullable String query, int clientLimit, int documentLimit);

}
