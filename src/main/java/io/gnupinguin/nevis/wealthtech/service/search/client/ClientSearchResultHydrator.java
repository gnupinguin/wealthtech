package io.gnupinguin.nevis.wealthtech.service.search.client;

import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import org.jspecify.annotations.NonNull;

import java.util.List;

public interface ClientSearchResultHydrator {

    @NonNull
    List<ClientSearchResult> hydrate(@NonNull List<ClientSearchProjection> searchResults);

}
