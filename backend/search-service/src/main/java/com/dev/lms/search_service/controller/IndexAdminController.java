package com.dev.lms.search_service.controller;

import com.dev.lms.common.response.ApiResponse;
import com.dev.lms.search_service.event.CourseIndexEvent;
import com.dev.lms.search_service.service.CourseIndexService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/search/admin/index")
public class IndexAdminController {

    private final CourseIndexService indexService;

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<Void>> indexCourse(@RequestBody CourseIndexEvent event) {
        indexService.index(event);
        return ResponseEntity.ok(ApiResponse.ok("indexed"));
    }

    @PostMapping("/courses/bulk")
    public ResponseEntity<ApiResponse<Integer>> bulkIndex(@RequestBody List<CourseIndexEvent> events) {
        int count = 0;
        for (CourseIndexEvent e : events) {
            indexService.index(e);
            count++;
        }
        return ResponseEntity.ok(ApiResponse.ok("indexed " + count, count));
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable String courseId) {
        indexService.deleteById(courseId);
        return ResponseEntity.ok(ApiResponse.ok("deleted"));
    }
}
