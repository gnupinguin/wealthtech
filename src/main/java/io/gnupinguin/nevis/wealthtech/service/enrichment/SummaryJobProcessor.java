package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SummaryJobProcessor implements DocumentEnrichmentJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(SummaryJobProcessor.class);

    @Override
    public JobType type() {
        return JobType.SUMMARY;
    }

    @Override
    public void process(DocumentEnrichmentJobEntity job) {
        log.info("Processing SUMMARY job {} for document {}", job.id(), job.documentId());
        // TODO: generate summary via AI and persist to documents.summary
    }
}
