package com.dev.lms.course_service.draft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDraft {
    private String lessonId;
    private String title;
    private String lessonType;
    private String videoUrl;
    private Integer durationMinutes;
    private Boolean isPreview;
    private String textUrl;
}
