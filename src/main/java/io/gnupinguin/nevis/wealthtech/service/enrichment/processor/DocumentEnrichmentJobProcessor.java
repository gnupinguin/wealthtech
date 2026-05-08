package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import org.jspecify.annotations.NonNull;

public interface DocumentEnrichmentJobProcessor {

    @NonNull
    JobType type();

    void process(@NonNull DocumentEnrichmentJobEntity job);
}
