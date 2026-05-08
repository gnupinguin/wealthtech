package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import org.jspecify.annotations.NonNull;

public interface DocumentEnrichmentJobProcessor {

    @NonNull
    JobType type();

    void process(@NonNull DocumentEnrichmentJobEntity job);
}
