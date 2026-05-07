package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.model.SearchResponse;
import io.gnupinguin.nevis.wealthtech.service.SemanticSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchEndpoint {

    private final SemanticSearchService searchService;

    public SearchEndpoint(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        return searchService.search(q);
    }
}
