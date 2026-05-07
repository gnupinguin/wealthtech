package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import io.gnupinguin.nevis.wealthtech.repository.DocumentEnrichmentJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DocumentEnrichmentScheduler {

    private final DocumentEnrichmentJobRepository jobRepository;
    private final Map<JobType, DocumentEnrichmentJobProcessor> processors;
    private final ThreadPoolTaskExecutor processorExecutor;
    private final long processorTimeoutMs;

    public DocumentEnrichmentScheduler(
            DocumentEnrichmentJobRepository jobRepository,
            List<DocumentEnrichmentJobProcessor> processors,
            @Qualifier("enrichmentProcessorExecutor") ThreadPoolTaskExecutor processorExecutor,
            @Value("${enrichment.processor.timeout-ms:30000}") long processorTimeoutMs) {
        this.jobRepository = jobRepository;
        this.processors = processors.stream()
                .collect(Collectors.toMap(DocumentEnrichmentJobProcessor::type, Function.identity()));
        this.processorExecutor = processorExecutor;
        this.processorTimeoutMs = processorTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${enrichment.scheduler.fixed-delay-ms:5000}")
    public void processNextJob() {
        var jobHolder = jobRepository.tryLockNextPendingJob();
        if (jobHolder.isEmpty()) {
            log.info("There are no jobs for processing");
        } else {
            var job = jobHolder.get();
            log.info("Locked job {} (type={}, attempts={}/{})", job.id(), job.type(), job.attempts(), job.maxAttempts());
            runWithTimeout(job);
        }
    }

    private void runWithTimeout(DocumentEnrichmentJobEntity job) {
        var processor = processors.get(job.type());
        if (processor == null) {
            log.error("No processor found for job type {}", job.type());
            jobRepository.save(failed(job, "No processor registered for type: " + job.type()));
            return;
        }

        try {
            runJob(job, processor);
            jobRepository.save(completed(job));
            log.info("Job {}/{} completed successfully", job.id(), job.type());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                log.warn("Job {}/{} timed out after {}ms", job.id(), job.type(), processorTimeoutMs);
                handleFailure(job, "Processing timed out after " + processorTimeoutMs + "ms");
            } else {
                log.error("Job {}/{} failed: {}", job.id(), job.type(), cause.getMessage(), cause);
                handleFailure(job, cause.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Job {}/{} interrupted, requeueing", job.id(), job.type());
            jobRepository.save(requeuedWithBackoff(job, "Interrupted"));
        }
    }

    private void runJob(DocumentEnrichmentJobEntity job, DocumentEnrichmentJobProcessor processor) throws InterruptedException, ExecutionException {
        CompletableFuture.runAsync(() -> processor.process(job), processorExecutor)
                .orTimeout(processorTimeoutMs, TimeUnit.MILLISECONDS)
                .get();
    }

    private void handleFailure(DocumentEnrichmentJobEntity job, String error) {
        if (job.attempts() >= job.maxAttempts()) {
            jobRepository.save(failed(job, error));
        } else {
            jobRepository.save(requeuedWithBackoff(job, error));
        }
    }

    private static DocumentEnrichmentJobEntity completed(DocumentEnrichmentJobEntity job) {
        var now = Instant.now();
        return new DocumentEnrichmentJobEntity(
                job.id(), job.documentId(), job.type(), JobStatus.COMPLETED,
                job.attempts(), job.maxAttempts(), null,
                job.availableAt(), job.lockedAt(), now, job.createdAt(), now
        );
    }

    private static DocumentEnrichmentJobEntity failed(DocumentEnrichmentJobEntity job, String error) {
        var now = Instant.now();
        return new DocumentEnrichmentJobEntity(
                job.id(), job.documentId(), job.type(), JobStatus.FAILED,
                job.attempts(), job.maxAttempts(), error,
                job.availableAt(), job.lockedAt(), null, job.createdAt(), now
        );
    }

    private static DocumentEnrichmentJobEntity requeuedWithBackoff(DocumentEnrichmentJobEntity job, String error) {
        var now = Instant.now();
        var nextAvailableAt = now.plusSeconds(30L * job.attempts());
        return new DocumentEnrichmentJobEntity(
                job.id(), job.documentId(), job.type(), JobStatus.PENDING,
                job.attempts(), job.maxAttempts(), error,
                nextAvailableAt, null, null, job.createdAt(), now
        );
    }

}
