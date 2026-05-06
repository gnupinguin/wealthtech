package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;

public interface DocumentEnrichmentJobProcessor {

    JobType type();

    void process(DocumentEnrichmentJobEntity job);
}
