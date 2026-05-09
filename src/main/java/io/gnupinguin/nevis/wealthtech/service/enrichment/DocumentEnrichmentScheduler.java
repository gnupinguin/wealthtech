package io.gnupinguin.nevis.wealthtech.service.enrichment;

import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DocumentEnrichmentScheduler {

    private final DocumentEnrichmentJobDispatcher jobDispatcher;

    public DocumentEnrichmentScheduler(@NonNull DocumentEnrichmentJobDispatcher jobDispatcher) {
        this.jobDispatcher = jobDispatcher;
    }

    @Scheduled(fixedDelayString = "${enrichment.scheduler.fixed-delay-ms:5000}")
    public void processNextJob() {
        jobDispatcher.dispatchNextJobs();
    }

}
