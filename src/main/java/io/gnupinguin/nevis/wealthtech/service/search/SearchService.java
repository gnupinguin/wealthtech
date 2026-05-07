package io.gnupinguin.nevis.wealthtech.service.search;

import org.jspecify.annotations.NonNull;

import java.util.List;

public interface SearchService<T> {

    @NonNull
    List<T> search(@NonNull String query, int limit);

}
