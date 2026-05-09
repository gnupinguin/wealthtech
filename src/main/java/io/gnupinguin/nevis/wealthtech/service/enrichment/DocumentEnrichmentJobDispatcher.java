package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.config.EnrichmentProperties;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Slf4j
@Component
public class DocumentEnrichmentJobDispatcher {

    private final DocumentEnrichmentJobRepository jobRepository;
    private final DocumentEnrichmentJobRunner jobRunner;
    private final ThreadPoolTaskExecutor processorExecutor;
    private final Semaphore processorSlots;

    public DocumentEnrichmentJobDispatcher(
            @NonNull DocumentEnrichmentJobRepository jobRepository,
            @NonNull DocumentEnrichmentJobRunner jobRunner,
            @Qualifier("enrichmentProcessorExecutor") @NonNull ThreadPoolTaskExecutor processorExecutor,
            @NonNull EnrichmentProperties enrichmentProperties) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
        this.processorExecutor = processorExecutor;
        this.processorSlots = new Semaphore(enrichmentProperties.processor().poolSize());
    }

    public void dispatchNextJobs() {
        var freeSlots = processorSlots.drainPermits();
        if (freeSlots == 0) {
            log.debug("All enrichment processor slots are busy");
            return;
        }

        var jobs = jobRepository.tryLockNextPendingJobs(freeSlots);
        if (jobs.isEmpty()) {
            processorSlots.release(freeSlots);
            log.debug("There are no jobs for processing");
            return;
        }

        var unusedSlots = freeSlots - jobs.size();
        if (unusedSlots > 0) {
            processorSlots.release(unusedSlots);
        }

        var submittedJobs = 0;
        for (var job : jobs) {
            var submitted = false;
            try {
                log.info("Locked job {} (type={}, attempts={}/{})", job.id(), job.type(), job.attempts(), job.maxAttempts());
                submitted = submit(job);
                if (submitted) {
                    submittedJobs++;
                }
            } finally {
                if (!submitted) {
                    processorSlots.release();
                }
            }
        }

        log.debug("Submitted {} of {} locked enrichment jobs", submittedJobs, jobs.size());
    }

    private boolean submit(@NonNull DocumentEnrichmentJobEntity job) {
        try {
            processorExecutor.execute(() -> {
                try {
                    jobRunner.run(job);
                } finally {
                    processorSlots.release();
                }
            });
            return true;
        } catch (RuntimeException e) {
            log.error("Job {}/{} could not be submitted: {}", job.id(), job.type(), errorMessage(e), e);
            jobRunner.failBeforeProcessing(job, e);
            return false;
        }
    }

    private static @NonNull String errorMessage(@NonNull Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

}
