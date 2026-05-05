package com.dev.lms.search_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record SearchRequest(
        String query,
        List<String> categories,
        List<String> subcategories,
        List<String> languages,
        List<String> levels,
        Float minRating,
        PriceFilter price,
        List<DurationBucket> durations,
        SortOption sort,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public SearchRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null) sort = SortOption.RELEVANCE;
        if (price == null) price = PriceFilter.ALL;
    }
}
