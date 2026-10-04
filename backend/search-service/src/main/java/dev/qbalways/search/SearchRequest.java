package dev.qbalways.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank @Size(max = 300) String q,
        QuestionType type,
        SearchWithin within,
        @Size(max = 80) String category,
        @Min(0) @Max(10) Integer difficulty,
        @Min(1900) Integer minYear,
        @Max(2200) Integer maxYear,
        @Min(0) Integer page,
        @Min(1) @Max(50) Integer size,
        Boolean exact
) {
    public SearchRequest {
        type = type == null ? QuestionType.ALL : type;
        within = within == null ? SearchWithin.ALL : within;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        exact = exact == null ? Boolean.FALSE : exact;
    }

    public enum QuestionType {
        ALL, TOSSUP, BONUS
    }

    public enum SearchWithin {
        ALL, QUESTION, ANSWER
    }
}
