package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.rest.model.SearchResponse;
import io.gnupinguin.nevis.wealthtech.service.search.SearchFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchEndpoint {

    private final SearchFacade searchFacade;
    private final SearchResponseMapper searchResponseMapper;

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        var result = searchFacade.search(q);
        return searchResponseMapper.toResponse(q, result);
    }

}
