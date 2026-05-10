package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SummaryJobProcessor implements DocumentEnrichmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(SummaryJobProcessor.class);

    @Override
    public @NonNull JobType type() {
        return JobType.SUMMARY;
    }

    @Override
    public void process(@NonNull DocumentEnrichmentEvent event) {
        log.info("Processing SUMMARY event {} for document {}", event.id(), event.documentId());
        // TODO: generate summary via AI and persist to documents.summary
    }
}
