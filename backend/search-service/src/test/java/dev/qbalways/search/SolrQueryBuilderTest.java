package dev.qbalways.search;

import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

class SolrQueryBuilderTest {
    private final SolrQueryBuilder builder = new SolrQueryBuilder();

    @Test
    void boostsAnswerAndPhraseMatchesAndBuildsFilters() {
        SearchRequest request = new SearchRequest(
                "mitochondrial Eve",
                SearchRequest.QuestionType.TOSSUP,
                SearchRequest.SearchWithin.ALL,
                "Science",
                5,
                2020,
                2026,
                1,
                10,
                false
        );

        MultiValueMap<String, String> params = builder.build(request);

        assertThat(params.getFirst("qf")).contains("answer_text^4", "question_text^2");
        assertThat(params.getFirst("pf")).contains("answer_text^12");
        assertThat(params.get("fq")).containsExactly(
                "question_type:tossup",
                "category:\"Science\"",
                "difficulty:5",
                "set_year:[2020 TO 2026]"
        );
        assertThat(params.getFirst("start")).isEqualTo("10");
    }

    @Test
    void escapesUserControlledLuceneSyntax() {
        SearchRequest request = new SearchRequest(
                "title:(foo OR *:*)",
                null, null, null, null, null, null, null, null, true
        );

        MultiValueMap<String, String> params = builder.build(request);

        assertThat(params.getFirst("q"))
                .isEqualTo("\"title\\:\\(foo OR \\*\\:\\*\\)\"");
        assertThat(params.getFirst("uf")).isEqualTo("-*");
    }

    @Test
    void canLimitSearchToAnswerlines() {
        SearchRequest request = new SearchRequest(
                "mitochondria",
                SearchRequest.QuestionType.ALL,
                SearchRequest.SearchWithin.ANSWER,
                null, null, null, null, null, null, false
        );

        MultiValueMap<String, String> params = builder.build(request);

        assertThat(params.getFirst("qf")).isEqualTo("answer_text^4");
        assertThat(params.getFirst("pf")).isEqualTo("answer_text^12");
        assertThat(params.getFirst("facet.mincount")).isEqualTo("1");
    }
}
