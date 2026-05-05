package com.dev.lms.search_service.repository;

import com.dev.lms.search_service.document.SearchTermDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchTermRepository extends ElasticsearchRepository<SearchTermDocument, String> {
}
