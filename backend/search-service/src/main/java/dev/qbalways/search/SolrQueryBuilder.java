package dev.qbalways.search;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class SolrQueryBuilder {
    private static final String LUCENE_SPECIAL = "+-!(){}[]^\"~*?:\\/";

    public MultiValueMap<String, String> build(SearchRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        String escaped = escape(request.q().strip());

        params.add("q", request.exact() ? "\"" + escaped + "\"" : escaped);
        params.add("defType", "edismax");
        params.add("qf", queryFields(request.within()));
        params.add("pf", phraseFields(request.within()));
        params.add("pf2", phrasePairFields(request.within()));
        params.add("ps", "3");
        params.add("mm", request.exact() ? "100%" : "70%");
        params.add("uf", "-*");
        params.add("start", String.valueOf(request.page() * request.size()));
        params.add("rows", String.valueOf(request.size()));
        params.add("fl", "id,question_type,question_text,answer_text,set_name,set_year,packet_name,category,subcategory,difficulty,score");
        params.add("sort", "score desc,set_year desc");
        params.add("hl", "true");
        params.add("hl.method", "unified");
        params.add("hl.fl", "question_text,answer_text");
        params.add("hl.snippets", "2");
        params.add("hl.fragsize", "240");
        params.add("hl.simple.pre", "[[HIGHLIGHT]]");
        params.add("hl.simple.post", "[[/HIGHLIGHT]]");
        params.add("facet", "true");
        params.add("facet.field", "category");
        params.add("facet.limit", "20");
        params.add("facet.mincount", "1");
        params.add("wt", "json");

        if (request.type() != SearchRequest.QuestionType.ALL) {
            params.add("fq", "question_type:" + request.type().name().toLowerCase());
        }
        if (request.category() != null && !request.category().isBlank()) {
            params.add("fq", "category:\"" + escape(request.category().strip()) + "\"");
        }
        if (request.difficulty() != null) {
            params.add("fq", "difficulty:" + request.difficulty());
        }
        if (request.minYear() != null || request.maxYear() != null) {
            String min = request.minYear() == null ? "*" : request.minYear().toString();
            String max = request.maxYear() == null ? "*" : request.maxYear().toString();
            params.add("fq", "set_year:[" + min + " TO " + max + "]");
        }
        return params;
    }

    private static String queryFields(SearchRequest.SearchWithin within) {
        return switch (within) {
            case QUESTION -> "question_text^2";
            case ANSWER -> "answer_text^4";
            case ALL -> "answer_text^4 question_text^2 set_name^0.5";
        };
    }

    private static String phraseFields(SearchRequest.SearchWithin within) {
        return switch (within) {
            case QUESTION -> "question_text^7";
            case ANSWER -> "answer_text^12";
            case ALL -> "answer_text^12 question_text^7";
        };
    }

    private static String phrasePairFields(SearchRequest.SearchWithin within) {
        return switch (within) {
            case QUESTION -> "question_text^3";
            case ANSWER -> "answer_text^6";
            case ALL -> "answer_text^6 question_text^3";
        };
    }

    static String escape(String input) {
        StringBuilder escaped = new StringBuilder(input.length() + 8);
        for (char current : input.toCharArray()) {
            if (LUCENE_SPECIAL.indexOf(current) >= 0 || current == '&' || current == '|') {
                escaped.append('\\');
            }
            escaped.append(current);
        }
        return escaped.toString();
    }
}
