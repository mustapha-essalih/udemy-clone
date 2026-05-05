package com.dev.lms.search_service.dto;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        long total,
        int page,
        int size,
        List<CourseHit> hits,
        Map<String, List<FacetBucket>> facets,
        long tookMillis
) {}
