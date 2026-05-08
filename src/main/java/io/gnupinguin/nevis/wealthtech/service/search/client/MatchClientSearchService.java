package io.gnupinguin.nevis.wealthtech.service.search.client;

import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import io.gnupinguin.nevis.wealthtech.persistence.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchClientSearchService implements ClientSearchService {

    private final ClientRepository repository;
    private final QueryNormalizer queryNormalizer;

    @Override
    public @NonNull List<ClientSearchProjection> search(@NonNull String query, int limit) {
        var searchQuery = queryNormalizer.normalize(query);
        return repository.findMatching(searchQuery.query(), searchQuery.prefix(), limit);
    }

}
