package com.dev.lms.course_service.draft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionDraft {
    private String sectionId;
    private String title;
    private List<LessonDraft> lessons;
}
