package dev.qbalways.indexer;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.net.http.HttpClient;

@Component
public class QbReaderClient {
    private static final int MAX_ATTEMPTS = 4;
    private final RestClient restClient;
    private final Duration minimumRequestInterval;
    private long lastRequestNanos;

    public QbReaderClient(
            @Value("${qbalways.qbreader.url}") String baseUrl,
            @Value("${qbalways.qbreader.requests-per-second:12}") int requestsPerSecond
    ) {
        this.restClient = RestClient.builder()
                .requestFactory(http1RequestFactory())
                .baseUrl(baseUrl)
                .build();
        this.minimumRequestInterval = Duration.ofMillis(Math.max(50, 1000 / requestsPerSecond));
    }

    public JsonNode fetchSets() {
        return getWithRetry(uri -> uri
                .path("/set-list")
                .queryParam("expand", true)
                .queryParam("includeCounts", true)
                .build()).path("setList");
    }

    public JsonNode fetchPacket(String setName, int packetNumber) {
        return getWithRetry(uri -> uri
                .path("/packet")
                .queryParam("setName", setName)
                .queryParam("packetNumber", packetNumber)
                .build());
    }

    private JsonNode getWithRetry(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
        RestClientException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            throttle();
            try {
                JsonNode body = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                            throw new RestClientException("QBReader returned " + response.getStatusCode());
                        })
                        .body(JsonNode.class);
                if (body == null) throw new RestClientException("QBReader returned an empty response");
                return body;
            } catch (RestClientException error) {
                lastError = error;
                sleep(Duration.ofMillis(250L * (1L << (attempt - 1))));
            }
        }
        throw lastError == null ? new RestClientException("QBReader request failed") : lastError;
    }

    private synchronized void throttle() {
        long remaining = minimumRequestInterval.toNanos() - (System.nanoTime() - lastRequestNanos);
        if (remaining > 0) sleep(Duration.ofNanos(remaining));
        lastRequestNanos = System.nanoTime();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Indexing interrupted", interrupted);
        }
    }

    private static JdkClientHttpRequestFactory http1RequestFactory() {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        return new JdkClientHttpRequestFactory(client);
    }
}
