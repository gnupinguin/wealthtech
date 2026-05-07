package io.gnupinguin.nevis.wealthtech.service.search.client;

import org.jspecify.annotations.NonNull;

public interface QueryNormalizer {

    @NonNull
    NormalizedQuery normalize(@NonNull String query);

}
