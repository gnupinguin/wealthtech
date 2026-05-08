package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.rest.model.SearchResponse;
import io.gnupinguin.nevis.wealthtech.rest.validation.SearchRequestValidator;
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
    private final SearchRequestValidator searchRequestValidator;

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q,
                                 @RequestParam(defaultValue = "5") int clientLimit,
                                 @RequestParam(defaultValue = "10") int documentLimit) {
        searchRequestValidator.validate(q, clientLimit, documentLimit);

        var result = searchFacade.search(q, clientLimit, documentLimit);
        return searchResponseMapper.toResponse(q, result);
    }

}
