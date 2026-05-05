package com.dev.lms.search_service.service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import com.dev.lms.search_service.document.CourseDocument;
import com.dev.lms.search_service.document.SearchTermDocument;
import com.dev.lms.search_service.dto.AutocompleteResponse;
import com.dev.lms.search_service.dto.AutocompleteResponse.Suggestion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AutocompleteService {

    private final ElasticsearchOperations operations;
    private final String coursesIndex;
    private final String suggestionsIndex;
    private final int maxSuggestions;

    public AutocompleteService(
            ElasticsearchOperations operations,
            @Value("${search.index.courses}") String coursesIndex,
            @Value("${search.index.suggestions}") String suggestionsIndex,
            @Value("${search.autocomplete.max-suggestions:8}") int maxSuggestions) {
        this.operations = operations;
        this.coursesIndex = coursesIndex;
        this.suggestionsIndex = suggestionsIndex;
        this.maxSuggestions = maxSuggestions;
    }

    public AutocompleteResponse autocomplete(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return new AutocompleteResponse(List.of(), List.of(), List.of(), List.of());
        }
        List<Suggestion> courses = courseSuggestions(prefix);
        List<Suggestion> instructors = instructorSuggestions(prefix);
        List<Suggestion> categories = categorySuggestions(prefix);
        List<Suggestion> popular = popularSuggestions(prefix);
        return new AutocompleteResponse(courses, instructors, categories, popular);
    }

    private List<Suggestion> courseSuggestions(String prefix) {
        FieldSuggester field = FieldSuggester.of(b -> b
                .prefix(prefix)
                .completion(c -> c
                        .field("titleSuggest")
                        .skipDuplicates(true)
                        .size(maxSuggestions)
                        .fuzzy(f -> f.fuzziness("AUTO"))));
        Suggester suggester = Suggester.of(s -> s.suggesters("course-suggest", field));

        NativeQuery query = NativeQuery.builder()
                .withSuggester(suggester)
                .build();

        SearchHits<CourseDocument> hits = operations.search(query, CourseDocument.class,
                IndexCoordinates.of(coursesIndex));
        return extractCompletionSuggestions(hits, "course-suggest", "course");
    }

    private List<Suggestion> instructorSuggestions(String prefix) {
        FieldSuggester field = FieldSuggester.of(b -> b
                .prefix(prefix)
                .completion(c -> c
                        .field("instructorSuggest")
                        .skipDuplicates(true)
                        .size(maxSuggestions)
                        .fuzzy(f -> f.fuzziness("AUTO"))));
        Suggester suggester = Suggester.of(s -> s.suggesters("instructor-suggest", field));

        NativeQuery query = NativeQuery.builder()
                .withSuggester(suggester)
                .build();

        SearchHits<CourseDocument> hits = operations.search(query, CourseDocument.class,
                IndexCoordinates.of(coursesIndex));
        return extractCompletionSuggestions(hits, "instructor-suggest", "instructor");
    }

    private List<Suggestion> categorySuggestions(String prefix) {
        String lower = prefix.toLowerCase();
        Aggregation agg = Aggregation.of(a -> a.terms(t -> t
                .field("category.raw")
                .size(maxSuggestions)
                .include(i -> i.regexp(".*" + java.util.regex.Pattern.quote(lower) + ".*"))));

        Query matchQuery = Query.of(q -> q.prefix(p -> p.field("category").value(lower)));

        NativeQuery query = NativeQuery.builder()
                .withQuery(matchQuery)
                .withPageable(PageRequest.of(0, 0))
                .withAggregation("categories", agg)
                .build();

        SearchHits<CourseDocument> hits = operations.search(query, CourseDocument.class,
                IndexCoordinates.of(coursesIndex));
        return extractTermsAgg(hits, "categories", "category");
    }

    private List<Suggestion> popularSuggestions(String prefix) {
        FieldSuggester field = FieldSuggester.of(b -> b
                .prefix(prefix)
                .completion(c -> c
                        .field("termSuggest")
                        .skipDuplicates(true)
                        .size(maxSuggestions)
                        .fuzzy(f -> f.fuzziness("AUTO"))));
        Suggester suggester = Suggester.of(s -> s.suggesters("term-suggest", field));

        NativeQuery query = NativeQuery.builder()
                .withSuggester(suggester)
                .build();
        try {
            SearchHits<SearchTermDocument> hits = operations.search(query, SearchTermDocument.class,
                    IndexCoordinates.of(suggestionsIndex));
            return extractCompletionSuggestionsForTerms(hits);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private <T> List<Suggestion> extractCompletionSuggestions(SearchHits<T> hits, String suggesterName, String type) {
        List<Suggestion> out = new ArrayList<>();
        Suggest suggest = hits.getSuggest();
        if (suggest == null) return out;

        var suggestion = suggest.getSuggestion(suggesterName);
        if (suggestion instanceof CompletionSuggestion<?> cs) {
            for (var entry : cs.getEntries()) {
                for (var option : entry.getOptions()) {
                    out.add(new Suggestion(option.getText(), type, null));
                }
            }
        }
        return out;
    }

    private List<Suggestion> extractCompletionSuggestionsForTerms(SearchHits<SearchTermDocument> hits) {
        List<Suggestion> out = new ArrayList<>();
        Suggest suggest = hits.getSuggest();
        if (suggest == null) return out;

        var suggestion = suggest.getSuggestion("term-suggest");
        if (suggestion instanceof CompletionSuggestion<?> cs) {
            for (var entry : cs.getEntries()) {
                for (var option : entry.getOptions()) {
                    out.add(new Suggestion(option.getText(), "popular", null));
                }
            }
        }
        return out;
    }

    private <T> List<Suggestion> extractTermsAgg(SearchHits<T> hits, String aggName, String type) {
        List<Suggestion> out = new ArrayList<>();
        var aggregations = hits.getAggregations();
        if (aggregations == null) return out;

        Object raw = aggregations.aggregations();
        if (!(raw instanceof List<?> list)) return out;

        for (Object item : list) {
            try {
                var elasticAgg = (org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation) item;
                if (!aggName.equals(elasticAgg.aggregation().getName())) continue;
                var aggregate = elasticAgg.aggregation().getAggregate();
                if (aggregate.isSterms()) {
                    StringTermsAggregate terms = aggregate.sterms();
                    for (var bucket : terms.buckets().array()) {
                        out.add(new Suggestion(bucket.key().stringValue(), type, null));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }
}
