package dev.qbalways.search;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SolrSearchClient searchClient;

    public SearchController(SolrSearchClient searchClient) {
        this.searchClient = searchClient;
    }

    @GetMapping
    public SearchResponse search(@Valid SearchRequest request) {
        return searchClient.search(request);
    }

    @RestControllerAdvice
    static class SearchErrorHandler {
        @ExceptionHandler({RestClientException.class, SearchUnavailableException.class})
        ProblemDetail unavailable(Exception error) {
            ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
            detail.setTitle("Search backend unavailable");
            detail.setDetail("QBAlways could not reach its search index.");
            return detail;
        }
    }
}
