package dev.qbalways.search;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;

@Component
public class SolrSearchClient {
    private final RestClient restClient;
    private final SolrQueryBuilder queryBuilder;

    public SolrSearchClient(
            SolrQueryBuilder queryBuilder,
            @Value("${qbalways.solr.url}") String solrUrl,
            @Value("${qbalways.solr.collection}") String collection
    ) {
        this.restClient = RestClient.builder()
                .requestFactory(http1RequestFactory())
                .baseUrl(solrUrl + "/solr/" + collection)
                .build();
        this.queryBuilder = queryBuilder;
    }

    public SearchResponse search(SearchRequest request) {
        MultiValueMap<String, String> params = queryBuilder.build(request);
        JsonNode root = restClient.get()
                .uri(uri -> uri.path("/select").queryParams(params).build())
                .retrieve()
                .body(JsonNode.class);

        if (root == null) throw new SearchUnavailableException("Solr returned an empty response");

        JsonNode response = root.path("response");
        JsonNode highlighting = root.path("highlighting");
        List<SearchResponse.SearchHit> hits = new ArrayList<>();

        for (JsonNode document : response.path("docs")) {
            String id = text(document, "id");
            List<String> snippets = new ArrayList<>();
            JsonNode highlighted = highlighting.path(id);
            addStrings(snippets, highlighted.path("question_text"));
            addStrings(snippets, highlighted.path("answer_text"));

            hits.add(new SearchResponse.SearchHit(
                    id,
                    text(document, "question_type"),
                    text(document, "question_text"),
                    text(document, "answer_text"),
                    text(document, "set_name"),
                    integer(document, "set_year"),
                    text(document, "packet_name"),
                    text(document, "category"),
                    text(document, "subcategory"),
                    integer(document, "difficulty"),
                    List.copyOf(snippets),
                    document.path("score").asDouble(0)
            ));
        }

        return new SearchResponse(
                request.q(),
                response.path("numFound").asLong(),
                request.page(),
                request.size(),
                root.path("responseHeader").path("QTime").asLong(),
                List.copyOf(hits),
                parseCategoryFacets(root)
        );
    }

    private static Map<String, Long> parseCategoryFacets(JsonNode root) {
        JsonNode values = root.path("facet_counts").path("facet_fields").path("category");
        Map<String, Long> facets = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.size(); index += 2) {
            facets.put(values.get(index).asText(), values.get(index + 1).asLong());
        }
        return facets;
    }

    private static void addStrings(List<String> output, JsonNode values) {
        if (values.isArray()) values.forEach(value -> output.add(value.asText()));
    }

    private static String text(JsonNode document, String field) {
        JsonNode value = document.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode document, String field) {
        JsonNode value = document.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private static JdkClientHttpRequestFactory http1RequestFactory() {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        return new JdkClientHttpRequestFactory(client);
    }
}
