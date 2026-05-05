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
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.time.Instant;
import java.util.List;

@Document(indexName = "#{@searchIndexName}", createIndex = false)
@Setting(settingPath = "elasticsearch/courses-settings.json")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocument {

    @Id
    @Field(type = FieldType.Keyword, name = "course_id")
    private String courseId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "english_analyzer", name = "title"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword),
                    @InnerField(suffix = "search_as_you_type", type = FieldType.Search_As_You_Type)
            }
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "english_analyzer", name = "description")
    private String description;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard", name = "instructor_name"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword),
                    @InnerField(suffix = "search_as_you_type", type = FieldType.Search_As_You_Type)
            }
    )
    private String instructorName;

    @Field(type = FieldType.Keyword, name = "instructor_id")
    private String instructorId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard", name = "category"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String category;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard", name = "subcategory"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String subcategory;

    @Field(type = FieldType.Float, name = "rating")
    private Float rating;

    @Field(type = FieldType.Long, name = "rating_count")
    private Long ratingCount;

    @Field(type = FieldType.Long, name = "enrollments")
    private Long enrollments;

    @Field(type = FieldType.Keyword, name = "language")
    private String language;

    @Field(type = FieldType.Integer, name = "duration_minutes")
    private Integer durationMinutes;

    @Field(type = FieldType.Float, name = "price")
    private Float price;

    @Field(type = FieldType.Boolean, name = "is_free")
    private Boolean isFree;

    @Field(type = FieldType.Keyword, name = "level")
    private String level;

    @Field(type = FieldType.Keyword, name = "status")
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date_time, name = "created_at")
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time, name = "updated_at")
    private Instant updatedAt;

    @Field(type = FieldType.Keyword, name = "tags")
    private List<String> tags;

    @CompletionField(maxInputLength = 100, analyzer = "simple", searchAnalyzer = "simple")
    private Completion titleSuggest;

    @CompletionField(maxInputLength = 100, analyzer = "simple", searchAnalyzer = "simple")
    private Completion instructorSuggest;
}
