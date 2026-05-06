package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChunkingJobProcessor implements DocumentEnrichmentJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(ChunkingJobProcessor.class);

    @Override
    public JobType type() {
        return JobType.CHUNKING;
    }

    @Override
    public void process(DocumentEnrichmentJobEntity job) {
        log.info("Processing CHUNKING job {} for document {}", job.id(), job.documentId());
        // TODO: split document into chunks, generate embeddings, persist to document_chunks
    }
}
