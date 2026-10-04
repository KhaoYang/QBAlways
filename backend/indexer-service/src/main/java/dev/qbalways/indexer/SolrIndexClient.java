package dev.qbalways.indexer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;

@Component
public class SolrIndexClient {
    private final RestClient collectionClient;
    private final RestClient schemaClient;

    public SolrIndexClient(
            @Value("${qbalways.solr.url}") String solrUrl,
            @Value("${qbalways.solr.collection}") String collection
    ) {
        String collectionUrl = solrUrl + "/solr/" + collection;
        JdkClientHttpRequestFactory requestFactory = http1RequestFactory();
        this.collectionClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(collectionUrl)
                .build();
        this.schemaClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(collectionUrl + "/schema")
                .build();
    }

    public void ensureSchema() {
        for (FieldDefinition field : fields()) {
            try {
                schemaClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("add-field", field.asMap()))
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException.BadRequest error) {
                if (!error.getResponseBodyAsString().contains("already exists")) throw error;
            }
        }
    }

    public void index(List<Map<String, Object>> documents) {
        if (documents.isEmpty()) return;
        collectionClient.post()
                .uri("/update?commitWithin=5000&overwrite=true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(documents)
                .retrieve()
                .toBodilessEntity();
    }

    public void commit() {
        collectionClient.post()
                .uri("/update?commit=true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .toBodilessEntity();
    }

    private static List<FieldDefinition> fields() {
        return List.of(
                new FieldDefinition("source_id", "string", true),
                new FieldDefinition("question_type", "string", true),
                new FieldDefinition("question_text", "text_general", false),
                new FieldDefinition("answer_text", "text_general", false),
                new FieldDefinition("set_name", "text_general", false),
                new FieldDefinition("set_year", "pint", true),
                new FieldDefinition("standard", "boolean", true),
                new FieldDefinition("packet_name", "string", true),
                new FieldDefinition("category", "string", true),
                new FieldDefinition("subcategory", "string", true),
                new FieldDefinition("difficulty", "pint", true),
                new FieldDefinition("updated_at", "pdate", true)
        );
    }

    private record FieldDefinition(String name, String type, boolean docValues) {
        Map<String, Object> asMap() {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("name", name);
            field.put("type", type);
            field.put("indexed", true);
            field.put("stored", true);
            field.put("multiValued", false);
            field.put("docValues", docValues);
            return field;
        }
    }

    private static JdkClientHttpRequestFactory http1RequestFactory() {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        return new JdkClientHttpRequestFactory(client);
    }
}
