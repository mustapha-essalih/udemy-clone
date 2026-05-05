package com.dev.lms.search_service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.time.Instant;

@Document(indexName = "#{@suggestionIndexName}", createIndex = false)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchTermDocument {

    @Id
    @Field(type = FieldType.Keyword, name = "term_id")
    private String termId;

    @Field(type = FieldType.Keyword, name = "term")
    private String term;

    @Field(type = FieldType.Long, name = "frequency")
    private Long frequency;

    @Field(type = FieldType.Date, format = DateFormat.date_time, name = "last_searched_at")
    private Instant lastSearchedAt;

    @CompletionField(maxInputLength = 100, analyzer = "simple", searchAnalyzer = "simple")
    private Completion termSuggest;
}
