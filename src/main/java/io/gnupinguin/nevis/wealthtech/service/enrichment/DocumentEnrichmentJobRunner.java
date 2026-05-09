package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import io.gnupinguin.nevis.wealthtech.service.enrichment.processor.DocumentEnrichmentJobProcessor;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DocumentEnrichmentJobRunner {

    private final DocumentEnrichmentJobRepository jobRepository;
    private final Map<JobType, DocumentEnrichmentJobProcessor> processors;
    private final DocumentEnrichmentJobMetricsRecorder metricsRecorder;

    public DocumentEnrichmentJobRunner(
            @NonNull DocumentEnrichmentJobRepository jobRepository,
            @NonNull List<DocumentEnrichmentJobProcessor> processors,
            @NonNull DocumentEnrichmentJobMetricsRecorder metricsRecorder) {
        this.jobRepository = jobRepository;
        this.processors = processors.stream()
                .collect(Collectors.toMap(DocumentEnrichmentJobProcessor::type, Function.identity()));
        this.metricsRecorder = metricsRecorder;
    }

    public void run(@NonNull DocumentEnrichmentJobEntity job) {
        var processor = processors.get(job.type());
        if (processor == null) {
            log.error("No processor found for job type {}", job.type());
            jobRepository.save(failed(job, "No processor registered for type: " + job.type()));
            return;
        }

        Sample sample = metricsRecorder.startSample();
        try {
            processor.process(job);
            jobRepository.save(completed(job));
            metricsRecorder.recordCompleted(sample, job.type());
            log.info("Job {}/{} completed successfully", job.id(), job.type());
        } catch (RuntimeException e) {
            log.error("Job {}/{} failed: {}", job.id(), job.type(), errorMessage(e), e);
            handleFailure(job, errorMessage(e), sample);
        }
    }

    void failBeforeProcessing(@NonNull DocumentEnrichmentJobEntity job, @NonNull RuntimeException e) {
        handleFailure(job, errorMessage(e), metricsRecorder.startSample());
    }

    private void handleFailure(@NonNull DocumentEnrichmentJobEntity job, @NonNull String error, @NonNull Sample sample) {
        if (job.attempts() >= job.maxAttempts()) {
            jobRepository.save(failed(job, error));
            metricsRecorder.recordFailed(sample, job.type());
        } else {
            jobRepository.save(requeuedWithBackoff(job, error));
            metricsRecorder.recordRequeued(sample, job.type());
        }
    }

    private static @NonNull DocumentEnrichmentJobEntity completed(@NonNull DocumentEnrichmentJobEntity job) {
        var now = Instant.now();
        return new DocumentEnrichmentJobEntity(
                job.id(), job.documentId(), job.type(), JobStatus.COMPLETED,
                job.attempts(), job.maxAttempts(), null,
                job.availableAt(), job.lockedAt(), now, job.createdAt(), now
        );
    }

    private static @NonNull DocumentEnrichmentJobEntity failed(@NonNull DocumentEnrichmentJobEntity job, @NonNull String error) {
        var now = Instant.now();
        return new DocumentEnrichmentJobEntity(
                job.id(), job.documentId(), job.type(), JobStatus.FAILED,
                job.attempts(), job.maxAttempts(), error,
                job.availableAt(), job.lockedAt(), null, job.createdAt(), now
        );
    }

    private static @NonNull DocumentEnrichmentJobEntity requeuedWithBackoff(@NonNull DocumentEnrichmentJobEntity job, @NonNull String error) {
        var now = Instant.now();
        var nextAvailableAt = now.plusSeconds(30L * job.attempts());
        return new DocumentEnrichmentJobEntity(
                job.id(), job.documentId(), job.type(), JobStatus.PENDING,
                job.attempts(), job.maxAttempts(), error,
                nextAvailableAt, null, null, job.createdAt(), now
        );
    }

    private static @NonNull String errorMessage(@NonNull Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

}
