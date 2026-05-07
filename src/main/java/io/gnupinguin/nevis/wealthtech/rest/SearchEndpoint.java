package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.model.ClientSearchResponse;
import io.gnupinguin.nevis.wealthtech.model.DocumentSearchResponse;
import io.gnupinguin.nevis.wealthtech.model.SearchResponse;
import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.SocialLink;
import io.gnupinguin.nevis.wealthtech.service.search.ScoredEntity;
import io.gnupinguin.nevis.wealthtech.service.search.SearchFacade;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchEntity;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchEndpoint {

    private final SearchFacade searchFacade;

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        var result = searchFacade.search(q);

        List<ClientSearchResponse> clients = result.clients().stream()
                .map(SearchEndpoint::convert)
                .toList();

        List<DocumentSearchResponse> documents = result.documents().stream()
                .map(SearchEndpoint::convert)
                .toList();

        return new SearchResponse(q, clients, documents);
    }

    private static @NonNull DocumentSearchResponse convert(DocumentSearchEntity d) {
        return new DocumentSearchResponse(d.id(), d.clientId(), d.score(), d.title(), d.summary(), d.matchedChunk());
    }

    private static @NonNull ClientSearchResponse convert(ScoredEntity<ClientEntity> scored) {
        var c = scored.entity();
        var links = c.socialLinks().stream().map(SocialLink::url).toList();
        return new ClientSearchResponse(c.id(), c.firstName(), c.lastName(), c.email(), c.description(), links, scored.score());
    }

}
