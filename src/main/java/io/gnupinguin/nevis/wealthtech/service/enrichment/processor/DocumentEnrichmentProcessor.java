package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import org.jspecify.annotations.NonNull;

public interface DocumentEnrichmentProcessor {

    @NonNull
    JobType type();

    void process(@NonNull DocumentEnrichmentEvent event);
}
