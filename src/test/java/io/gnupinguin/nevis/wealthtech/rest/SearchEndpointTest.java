package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.exception.BadRequestException;
import io.gnupinguin.nevis.wealthtech.exception.ServiceUnavailableException;
import io.gnupinguin.nevis.wealthtech.rest.dto.SearchResponse;
import io.gnupinguin.nevis.wealthtech.rest.validation.SearchRequestValidator;
import io.gnupinguin.nevis.wealthtech.service.search.SearchFacade;
import io.gnupinguin.nevis.wealthtech.service.search.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchEndpointTest {

    @Mock
    private SearchFacade searchFacade;

    @Mock
    private SearchResponseMapper searchResponseMapper;

    @Mock
    private SearchRequestValidator searchRequestValidator;

    @InjectMocks
    private SearchEndpoint searchEndpoint;

    @Test
    void testSearchDelegatesToValidatorWithCorrectParameters() {
        var result = new SearchResult(List.of(), List.of(), List.of());
        var response = new SearchResponse("growth", List.of(), List.of(), List.of());
        when(searchFacade.search("growth", 5, 10)).thenReturn(result);
        when(searchResponseMapper.toResponse("growth", result)).thenReturn(response);

        searchEndpoint.search("growth", 5, 10);

        verify(searchRequestValidator).validate("growth", 5, 10);
    }

    @Test
    void testSearchDelegatesToFacadeWithCorrectParameters() {
        var result = new SearchResult(List.of(), List.of(), List.of());
        var response = new SearchResponse("growth", List.of(), List.of(), List.of());
        when(searchFacade.search("growth", 5, 10)).thenReturn(result);
        when(searchResponseMapper.toResponse("growth", result)).thenReturn(response);

        searchEndpoint.search("growth", 5, 10);

        verify(searchFacade).search("growth", 5, 10);
    }

    @Test
    void testSearchDelegatesToMapperWithQueryAndFacadeResult() {
        var result = new SearchResult(List.of(), List.of(), List.of());
        var response = new SearchResponse("growth", List.of(), List.of(), List.of());
        when(searchFacade.search("growth", 5, 10)).thenReturn(result);
        when(searchResponseMapper.toResponse("growth", result)).thenReturn(response);

        searchEndpoint.search("growth", 5, 10);

        verify(searchResponseMapper).toResponse("growth", result);
    }

    @Test
    void testSearchReturnsMapperResponse() {
        var result = new SearchResult(List.of(), List.of(), List.of());
        var expected = new SearchResponse("growth", List.of(), List.of(), List.of());
        when(searchFacade.search("growth", 5, 10)).thenReturn(result);
        when(searchResponseMapper.toResponse("growth", result)).thenReturn(expected);

        var actual = searchEndpoint.search("growth", 5, 10);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void testSearchPropagatesValidationException() {
        doThrow(new BadRequestException("query is too short")).when(searchRequestValidator).validate("ab", 5, 10);

        assertThrows(BadRequestException.class, () -> searchEndpoint.search("ab", 5, 10));
        verifyNoInteractions(searchFacade, searchResponseMapper);
    }

    @Test
    void testSearchPropagatesServiceUnavailableException() {
        when(searchFacade.search("growth", 5, 10)).thenThrow(new ServiceUnavailableException("Service is unavailable"));

        assertThrows(ServiceUnavailableException.class, () -> searchEndpoint.search("growth", 5, 10));
        verifyNoInteractions(searchResponseMapper);
    }

}
