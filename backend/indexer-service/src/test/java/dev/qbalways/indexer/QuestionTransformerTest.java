package dev.qbalways.indexer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTransformerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuestionTransformer transformer = new QuestionTransformer();

    @Test
    void transformsTossupsAndBonusesIntoOneSearchDocumentPerQuestion() throws Exception {
        JsonNode packet = objectMapper.readTree("""
                {
                  "packet": {"name": "Packet 1"},
                  "tossups": [{
                    "_id": "t1", "question_sanitized": "A clue about mitochondria.",
                    "answer_sanitized": "mitochondrion", "category": "Science",
                    "subcategory": "Biology", "difficulty": 4,
                    "updatedAt": "2026-01-01T00:00:00Z"
                  }],
                  "bonuses": [{
                    "_id": "b1", "leadin_sanitized": "For 10 points each:",
                    "parts_sanitized": ["Name this organelle.", "Name its DNA."],
                    "answers_sanitized": ["mitochondrion", "mtDNA"],
                    "category": "Science", "subcategory": "Biology", "difficulty": 4
                  }]
                }
                """);
        JsonNode set = objectMapper.readTree("""
                {"setName": "2026 Test Set", "year": 2026, "standard": true}
                """);

        List<Map<String, Object>> documents = transformer.transformPacket(packet, set);

        assertThat(documents).hasSize(2);
        assertThat(documents.getFirst())
                .containsEntry("id", "tossup:t1")
                .containsEntry("question_type", "tossup")
                .containsEntry("set_year", 2026);
        assertThat(documents.get(1))
                .containsEntry("id", "bonus:b1")
                .containsEntry("question_text", "For 10 points each: Name this organelle. Name its DNA.")
                .containsEntry("answer_text", "mitochondrion • mtDNA");
    }
}
