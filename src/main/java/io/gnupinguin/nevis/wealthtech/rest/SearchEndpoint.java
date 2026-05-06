package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.model.ClientSearchResult;
import io.gnupinguin.nevis.wealthtech.model.DocumentSearchResult;
import io.gnupinguin.nevis.wealthtech.model.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SearchEndpoint {

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        var clientId = UUID.randomUUID();
        var documentId = UUID.randomUUID();

        return new SearchResponse(
                q,
                List.of(new ClientSearchResult(clientId, 0.95f)),
                List.of(new DocumentSearchResult(
                        documentId,
                        clientId,
                        0.87f,
                        "Mock Document Title",
                        "Mock document summary matching: " + q,
                        "...matched text excerpt for: " + q + "..."
                ))
        );
    }
}
