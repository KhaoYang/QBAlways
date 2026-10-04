package dev.qbalways.search;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        String query,
        long total,
        int page,
        int size,
        long tookMs,
        List<SearchHit> results,
        Map<String, Long> categories
) {
    public record SearchHit(
            String id,
            String questionType,
            String questionText,
            String answerText,
            String setName,
            Integer setYear,
            String packetName,
            String category,
            String subcategory,
            Integer difficulty,
            List<String> highlights,
            double score
    ) {}
}
