package com.dev.lms.search_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.search_service.dto.AutocompleteResponse;
import com.dev.lms.search_service.dto.DurationBucket;
import com.dev.lms.search_service.dto.PriceFilter;
import com.dev.lms.search_service.dto.SearchRequest;
import com.dev.lms.search_service.dto.SearchResponse;
import com.dev.lms.search_service.dto.SortOption;
import com.dev.lms.search_service.service.AutocompleteService;
import com.dev.lms.search_service.service.CourseIndexService;
import com.dev.lms.search_service.service.SearchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final AutocompleteService autocompleteService;
    private final CourseIndexService indexService;

    public SearchController(SearchService searchService,
                            AutocompleteService autocompleteService,
                            CourseIndexService indexService) {
        this.searchService = searchService;
        this.autocompleteService = autocompleteService;
        this.indexService = indexService;
    }

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "category", required = false) List<String> categories,
            @RequestParam(value = "subcategory", required = false) List<String> subcategories,
            @RequestParam(value = "language", required = false) List<String> languages,
            @RequestParam(value = "level", required = false) List<String> levels,
            @RequestParam(value = "minRating", required = false) Float minRating,
            @RequestParam(value = "price", required = false) PriceFilter price,
            @RequestParam(value = "duration", required = false) List<DurationBucket> durations,
            @RequestParam(value = "sort", required = false) SortOption sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {

        SearchRequest req = new SearchRequest(query, categories, subcategories, languages, levels,
                minRating, price, durations, sort, page, size);

        SearchResponse response = searchService.search(req);

        if (query != null && !query.isBlank()) {
            indexService.recordSearchTerm(query);
        }

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<AutocompleteResponse>> autocomplete(
            @RequestParam(value = "q") @NotBlank String prefix) {
        AutocompleteResponse response = autocompleteService.autocomplete(prefix);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
