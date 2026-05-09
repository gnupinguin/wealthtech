package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.concurrent.BoundedVirtualThreadExecutor;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DocumentEnrichmentJobDispatcher {

    private final DocumentEnrichmentJobRepository jobRepository;
    private final DocumentEnrichmentJobRunner jobRunner;
    private final BoundedVirtualThreadExecutor processorExecutor;

    public DocumentEnrichmentJobDispatcher(
            @NonNull DocumentEnrichmentJobRepository jobRepository,
            @NonNull DocumentEnrichmentJobRunner jobRunner,
            @Qualifier("enrichmentProcessorExecutor") @NonNull BoundedVirtualThreadExecutor processorExecutor) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
        this.processorExecutor = processorExecutor;
    }

    public void dispatchNextJobs() {
        var freeSlots = processorExecutor.drainAvailableSlots();
        if (freeSlots == 0) {
            log.debug("All enrichment processor slots are busy");
            return;
        }

        var jobs = lockJobs(freeSlots);
        if (jobs.isEmpty()) {
            processorExecutor.releaseSlots(freeSlots);
            log.debug("There are no jobs for processing");
            return;
        }

        var unusedSlots = freeSlots - jobs.size();
        if (unusedSlots > 0) {
            processorExecutor.releaseSlots(unusedSlots);
        }

        var submittedJobs = 0;
        for (var job : jobs) {
            log.info("Locked job {} (type={}, attempts={}/{})", job.id(), job.type(), job.attempts(), job.maxAttempts());
            if (submit(job)) {
                submittedJobs++;
            }
        }

        log.debug("Submitted {} of {} locked enrichment jobs", submittedJobs, jobs.size());
    }

    private boolean submit(@NonNull DocumentEnrichmentJobEntity job) {
        try {
            processorExecutor.executeReserved(() -> jobRunner.run(job));
            return true;
        } catch (RuntimeException e) {
            log.error("Job {}/{} could not be submitted: {}", job.id(), job.type(), errorMessage(e), e);
            jobRunner.failBeforeProcessing(job, e);
            return false;
        }
    }

    private @NonNull List<DocumentEnrichmentJobEntity> lockJobs(int freeSlots) {
        try {
            return jobRepository.tryLockNextPendingJobs(freeSlots);
        } catch (RuntimeException e) {
            processorExecutor.releaseSlots(freeSlots);
            throw e;
        }
    }

    private static @NonNull String errorMessage(@NonNull Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

}
