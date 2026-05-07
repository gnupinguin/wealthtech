package io.gnupinguin.nevis.wealthtech.service.search.client;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ClientQueryNormalizer implements QueryNormalizer {

    @Override
    public @NonNull NormalizedQuery normalize(@NonNull String query) {
        var normalized = query.trim().toLowerCase(Locale.ROOT);

        var escaped = normalized
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return new NormalizedQuery(normalized, escaped + "%");
    }

}
