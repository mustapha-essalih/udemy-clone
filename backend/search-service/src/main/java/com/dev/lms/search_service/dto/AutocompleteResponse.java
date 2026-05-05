package com.dev.lms.search_service.dto;

import java.util.List;

public record AutocompleteResponse(
        List<Suggestion> courses,
        List<Suggestion> instructors,
        List<Suggestion> categories,
        List<Suggestion> popularTerms
) {
    public record Suggestion(String text, String type, String referenceId) {}
}
