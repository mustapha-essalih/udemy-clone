package com.dev.lms.search_service.config;

import com.dev.lms.search_service.document.CourseDocument;
import com.dev.lms.search_service.document.SearchTermDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class IndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);
    private static final long INITIAL_DELAY_MS = 3_000;
    private static final long MAX_DELAY_MS = 30_000;

    private final ElasticsearchOperations operations;

    public IndexInitializer(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initIndexes() {
        ensureIndex(CourseDocument.class);
        ensureIndex(SearchTermDocument.class);
    }

    private void ensureIndex(Class<?> clazz) {
        long delay = INITIAL_DELAY_MS;
        int attempt = 0;

        while (true) {
            attempt++;
            try {
                IndexOperations idx = operations.indexOps(clazz);
                idx.createWithMapping();
                log.info("Created Elasticsearch index for {}", clazz.getSimpleName());
                return;
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : "";

                if (msg.contains("resource_already_exists_exception") || msg.contains("already exists")) {
                    log.info("Elasticsearch index for {} already exists — skipping creation", clazz.getSimpleName());
                    return;
                }

                log.warn("Elasticsearch not ready for {} (attempt {}), retrying in {}s — {}",
                        clazz.getSimpleName(), attempt, delay / 1000, rootCause(ex));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                delay = Math.min(delay * 2, MAX_DELAY_MS);
            }
        }
    }

    private String rootCause(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
