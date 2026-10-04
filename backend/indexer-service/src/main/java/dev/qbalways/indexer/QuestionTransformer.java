package dev.qbalways.indexer;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuestionTransformer {
    public List<Map<String, Object>> transformPacket(JsonNode packetResponse, JsonNode setMetadata) {
        List<Map<String, Object>> documents = new ArrayList<>();
        JsonNode packet = packetResponse.path("packet");
        packetResponse.path("tossups").forEach(question ->
                documents.add(transform(question, "tossup", packet, setMetadata)));
        packetResponse.path("bonuses").forEach(question ->
                documents.add(transform(question, "bonus", packet, setMetadata)));
        return documents;
    }

    private Map<String, Object> transform(
            JsonNode question,
            String questionType,
            JsonNode packet,
            JsonNode setMetadata
    ) {
        Map<String, Object> document = new LinkedHashMap<>();
        JsonNode embeddedSet = question.path("set");
        JsonNode embeddedPacket = question.path("packet");
        String sourceId = text(question, "_id");

        document.put("id", questionType + ":" + sourceId);
        document.put("source_id", sourceId);
        document.put("question_type", questionType);
        document.put("question_text", questionType.equals("tossup")
                ? text(question, "question_sanitized")
                : joinBonusText(question));
        document.put("answer_text", questionType.equals("tossup")
                ? text(question, "answer_sanitized")
                : joinArray(question.path("answers_sanitized")));
        document.put("set_name", firstText(embeddedSet, "name", setMetadata, "setName"));
        document.put("set_year", firstInteger(embeddedSet, "year", setMetadata, "year"));
        document.put("standard", firstBoolean(embeddedSet, "standard", setMetadata, "standard"));
        document.put("packet_name", firstText(embeddedPacket, "name", packet, "name"));
        document.put("category", text(question, "category"));
        document.put("subcategory", text(question, "subcategory"));
        document.put("difficulty", integer(question, "difficulty"));
        document.put("updated_at", text(question, "updatedAt"));
        document.values().removeIf(value -> value == null || value.toString().isBlank());
        return document;
    }

    private static String joinBonusText(JsonNode question) {
        List<String> parts = new ArrayList<>();
        String leadin = text(question, "leadin_sanitized");
        if (leadin != null) parts.add(leadin);
        question.path("parts_sanitized").forEach(part -> parts.add(part.asText()));
        return String.join(" ", parts);
    }

    private static String joinArray(JsonNode values) {
        List<String> output = new ArrayList<>();
        values.forEach(value -> output.add(value.asText()));
        return String.join(" • ", output);
    }

    private static String firstText(JsonNode first, String firstField, JsonNode second, String secondField) {
        String value = text(first, firstField);
        return value == null ? text(second, secondField) : value;
    }

    private static Integer firstInteger(JsonNode first, String firstField, JsonNode second, String secondField) {
        Integer value = integer(first, firstField);
        return value == null ? integer(second, secondField) : value;
    }

    private static Boolean firstBoolean(JsonNode first, String firstField, JsonNode second, String secondField) {
        JsonNode firstValue = first.path(firstField);
        if (firstValue.isBoolean()) return firstValue.asBoolean();
        JsonNode secondValue = second.path(secondField);
        return secondValue.isBoolean() ? secondValue.asBoolean() : null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isIntegralNumber() ? value.asInt() : null;
    }
}
