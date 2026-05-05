package com.dev.lms.search_service.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.DecayPlacement;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.dev.lms.search_service.document.CourseDocument;
import com.dev.lms.search_service.dto.CourseHit;
import com.dev.lms.search_service.dto.DurationBucket;
import com.dev.lms.search_service.dto.FacetBucket;
import com.dev.lms.search_service.dto.PriceFilter;
import com.dev.lms.search_service.dto.SearchRequest;
import com.dev.lms.search_service.dto.SearchResponse;
import com.dev.lms.search_service.dto.SortOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final ElasticsearchOperations operations;
    private final String coursesIndex;
    private final double recencyScaleDays;

    public SearchService(
            ElasticsearchOperations operations,
            @Value("${search.index.courses}") String coursesIndex,
            @Value("${search.ranking.recency-scale-days:90}") double recencyScaleDays) {
        this.operations = operations;
        this.coursesIndex = coursesIndex;
        this.recencyScaleDays = recencyScaleDays;
    }

    public SearchResponse search(SearchRequest req) {
        long start = System.currentTimeMillis();

        Query userQuery = buildUserQuery(req.query());
        Query rankedQuery = wrapWithRanking(userQuery, req.sort());
        Query filterQuery = buildFilterQuery(req);

        BoolQuery.Builder rootBool = new BoolQuery.Builder().must(rankedQuery);
        if (filterQuery != null) {
            rootBool.filter(filterQuery);
        }

        NativeQueryBuilder nq = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(rootBool.build())))
                .withPageable(PageRequest.of(req.page(), req.size()))
                .withTrackTotalHits(true)
                .withAggregation("by_category", categoryAgg())
                .withAggregation("by_language", languageAgg())
                .withAggregation("by_level", levelAgg());

        applySort(nq, req.sort());

        SearchHits<CourseDocument> hits = operations.search(
                nq.build(),
                CourseDocument.class,
                IndexCoordinates.of(coursesIndex)
        );

        List<CourseHit> results = hits.getSearchHits().stream()
                .map(this::toHit)
                .toList();

        Map<String, List<FacetBucket>> facets = extractFacets(hits);

        return new SearchResponse(
                hits.getTotalHits(),
                req.page(),
                req.size(),
                results,
                facets,
                System.currentTimeMillis() - start
        );
    }

    private Query buildUserQuery(String text) {
        if (text == null || text.isBlank()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        MultiMatchQuery mm = MultiMatchQuery.of(m -> m
                .query(text)
                .fields(
                        "title^6",
                        "title.search_as_you_type^3",
                        "title.search_as_you_type._2gram^2",
                        "title.search_as_you_type._3gram^2",
                        "instructor_name^4",
                        "instructor_name.search_as_you_type^2",
                        "category^3",
                        "subcategory^2",
                        "description^1",
                        "tags^2"
                )
                .type(TextQueryType.BestFields)
                .fuzziness("AUTO")
                .operator(Operator.Or)
                .minimumShouldMatch("70%")
                .tieBreaker(0.3)
        );
        return Query.of(q -> q.multiMatch(mm));
    }

    private Query wrapWithRanking(Query userQuery, SortOption sort) {
        if (sort != null && sort != SortOption.RELEVANCE) {
            return userQuery;
        }
        List<FunctionScore> functions = new ArrayList<>();

        functions.add(FunctionScore.of(f -> f
                .filter(Query.of(q -> q.exists(e -> e.field("rating"))))
                .fieldValueFactor(fv -> fv
                        .field("rating")
                        .factor(1.2)
                        .modifier(FieldValueFactorModifier.Sqrt)
                        .missing(0.0))
        ));

        functions.add(FunctionScore.of(f -> f
                .filter(Query.of(q -> q.exists(e -> e.field("enrollments"))))
                .fieldValueFactor(fv -> fv
                        .field("enrollments")
                        .factor(0.001)
                        .modifier(FieldValueFactorModifier.Log1p)
                        .missing(0.0))
        ));

        String scaleStr = ((long) recencyScaleDays) + "d";
        functions.add(FunctionScore.of(f -> f
                .gauss(g -> g.date(d -> d
                        .field("created_at")
                        .placement(DecayPlacement.<String, Time>of(p -> p
                                .origin(Instant.now().toString())
                                .scale(Time.of(t -> t.time(scaleStr)))
                                .offset(Time.of(t -> t.time("7d")))
                                .decay(0.5)))))
        ));

        FunctionScoreQuery fsq = FunctionScoreQuery.of(f -> f
                .query(userQuery)
                .functions(functions)
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Sum)
        );
        return Query.of(q -> q.functionScore(fsq));
    }

    private Query buildFilterQuery(SearchRequest req) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        boolean any = false;

        bool.filter(Query.of(q -> q.term(t -> t.field("status").value("PUBLISHED"))));
        any = true;

        if (req.categories() != null && !req.categories().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("category.raw")
                    .terms(tt -> tt.value(toFieldValues(req.categories()))))));
            any = true;
        }
        if (req.subcategories() != null && !req.subcategories().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("subcategory.raw")
                    .terms(tt -> tt.value(toFieldValues(req.subcategories()))))));
            any = true;
        }
        if (req.languages() != null && !req.languages().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("language")
                    .terms(tt -> tt.value(toFieldValues(req.languages()))))));
            any = true;
        }
        if (req.levels() != null && !req.levels().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("level")
                    .terms(tt -> tt.value(toFieldValues(req.levels()))))));
            any = true;
        }
        if (req.minRating() != null) {
            bool.filter(Query.of(q -> q.range(r -> r
                    .number(n -> n.field("rating").gte(req.minRating().doubleValue())))));
            any = true;
        }
        if (req.price() != null && req.price() != PriceFilter.ALL) {
            bool.filter(Query.of(q -> q.term(t -> t
                    .field("is_free")
                    .value(req.price() == PriceFilter.FREE))));
            any = true;
        }
        if (req.durations() != null && !req.durations().isEmpty()) {
            BoolQuery.Builder durationBool = new BoolQuery.Builder();
            for (DurationBucket b : req.durations()) {
                durationBool.should(Query.of(q -> q.range(r -> r
                        .number(n -> n
                                .field("duration_minutes")
                                .gte((double) b.getMinMinutes())
                                .lt((double) b.getMaxMinutes())))));
            }
            durationBool.minimumShouldMatch("1");
            bool.filter(Query.of(q -> q.bool(durationBool.build())));
            any = true;
        }
        return any ? Query.of(q -> q.bool(bool.build())) : null;
    }

    private void applySort(NativeQueryBuilder nq, SortOption sort) {
        if (sort == null || sort == SortOption.RELEVANCE) {
            return;
        }
        switch (sort) {
            case RATING -> nq.withSort(s -> s.field(f -> f.field("rating").order(SortOrder.Desc)));
            case NEWEST -> nq.withSort(s -> s.field(f -> f.field("created_at").order(SortOrder.Desc)));
            case POPULARITY -> nq.withSort(s -> s.field(f -> f.field("enrollments").order(SortOrder.Desc)));
            case PRICE_ASC -> nq.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
            case PRICE_DESC -> nq.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
            default -> {}
        }
    }

    private Aggregation categoryAgg() {
        return Aggregation.of(a -> a.terms(t -> t.field("category.raw").size(20)));
    }

    private Aggregation languageAgg() {
        return Aggregation.of(a -> a.terms(t -> t.field("language").size(20)));
    }

    private Aggregation levelAgg() {
        return Aggregation.of(a -> a.terms(t -> t.field("level").size(10)));
    }

    private Map<String, List<FacetBucket>> extractFacets(SearchHits<CourseDocument> hits) {
        Map<String, List<FacetBucket>> out = new HashMap<>();
        var aggregations = hits.getAggregations();
        if (aggregations == null) return out;

        Object raw = aggregations.aggregations();
        if (!(raw instanceof List<?> list)) return out;

        for (Object item : list) {
            try {
                var elasticAgg = (org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation) item;
                String name = elasticAgg.aggregation().getName();
                var aggregate = elasticAgg.aggregation().getAggregate();
                if (aggregate.isSterms()) {
                    StringTermsAggregate terms = aggregate.sterms();
                    List<FacetBucket> buckets = terms.buckets().array().stream()
                            .map(b -> new FacetBucket(b.key().stringValue(), b.docCount()))
                            .toList();
                    out.put(name, buckets);
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private List<FieldValue> toFieldValues(List<String> values) {
        return values.stream().map(FieldValue::of).toList();
    }

    private CourseHit toHit(SearchHit<CourseDocument> sh) {
        CourseDocument d = sh.getContent();
        return new CourseHit(
                d.getCourseId(),
                d.getTitle(),
                d.getDescription(),
                d.getInstructorName(),
                d.getInstructorId(),
                d.getCategory(),
                d.getSubcategory(),
                d.getRating(),
                d.getRatingCount(),
                d.getEnrollments(),
                d.getLanguage(),
                d.getDurationMinutes(),
                d.getPrice(),
                d.getIsFree(),
                d.getLevel(),
                d.getCreatedAt(),
                sh.getScore()
        );
    }
}
