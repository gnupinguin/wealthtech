package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.config.EnrichmentProperties;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import io.gnupinguin.nevis.wealthtech.service.enrichment.processor.DocumentEnrichmentJobProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentSchedulerTest {

    @Mock
    private DocumentEnrichmentJobRepository jobRepository;

    @Mock
    private ThreadPoolTaskExecutor processorExecutor;

    @Mock
    private DocumentEnrichmentJobProcessor chunkingProcessor;

    private DocumentEnrichmentScheduler scheduler;

    @BeforeEach
    void setUp() {
        var enrichmentProperties = new EnrichmentProperties(
                new EnrichmentProperties.Scheduler(5000L),
                new EnrichmentProperties.Processor(2, 5000L)
        );
        when(chunkingProcessor.type()).thenReturn(JobType.CHUNKING);
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(processorExecutor).execute(any(Runnable.class));
        scheduler = new DocumentEnrichmentScheduler(jobRepository, List.of(chunkingProcessor), processorExecutor, enrichmentProperties);
    }

    @Test
    void testProcessNextJobDoesNothingWhenNoJobsAvailable() {
        when(jobRepository.tryLockNextPendingJob()).thenReturn(Optional.empty());

        scheduler.processNextJob();

        verify(jobRepository, never()).save(any());
        verifyNoInteractions(chunkingProcessor);
    }

    @Test
    void testProcessNextJobCompletesJobOnSuccessfulProcessing() {
        var job = pendingJob(JobType.CHUNKING, 0, 3);
        when(jobRepository.tryLockNextPendingJob()).thenReturn(Optional.of(job));

        scheduler.processNextJob();

        verify(chunkingProcessor).process(job);
        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(captor.getValue().completedAt()).isNotNull();
        assertThat(captor.getValue().lastError()).isNull();
    }

    @Test
    void testProcessNextJobSavesFailedJobWhenNoProcessorRegistered() {
        var job = pendingJob(JobType.SUMMARY, 0, 3);
        when(jobRepository.tryLockNextPendingJob()).thenReturn(Optional.of(job));

        scheduler.processNextJob();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.FAILED);
        assertThat(captor.getValue().lastError()).contains("No processor registered");
        verifyNoInteractions(chunkingProcessor);
    }

    @Test
    void testProcessNextJobRequeuesJobWithBackoffWhenProcessingFailsAndAttemptsRemain() {
        var job = pendingJob(JobType.CHUNKING, 1, 3);
        when(jobRepository.tryLockNextPendingJob()).thenReturn(Optional.of(job));
        doThrow(new RuntimeException("processing error")).when(chunkingProcessor).process(job);

        scheduler.processNextJob();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(JobStatus.PENDING);
        assertThat(saved.lastError()).isEqualTo("processing error");
        assertThat(saved.availableAt()).isAfter(Instant.now().minusSeconds(1));
        assertThat(saved.lockedAt()).isNull();
    }

    @Test
    void testProcessNextJobSavesFailedJobWhenMaxAttemptsReached() {
        var job = pendingJob(JobType.CHUNKING, 3, 3);
        when(jobRepository.tryLockNextPendingJob()).thenReturn(Optional.of(job));
        doThrow(new RuntimeException("terminal error")).when(chunkingProcessor).process(job);

        scheduler.processNextJob();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.FAILED);
        assertThat(captor.getValue().lastError()).isEqualTo("terminal error");
    }

    @Test
    void testProcessNextJobRequeuesJobOnTimeoutWhenAttemptsRemain() {
        var enrichmentProperties = new EnrichmentProperties(
                new EnrichmentProperties.Scheduler(5000L),
                new EnrichmentProperties.Processor(2, 1L)
        );
        doNothing().when(processorExecutor).execute(any(Runnable.class));
        scheduler = new DocumentEnrichmentScheduler(jobRepository, List.of(chunkingProcessor), processorExecutor, enrichmentProperties);

        var job = pendingJob(JobType.CHUNKING, 0, 3);
        when(jobRepository.tryLockNextPendingJob()).thenReturn(Optional.of(job));

        scheduler.processNextJob();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.PENDING);
        assertThat(captor.getValue().lastError()).contains("timed out");
    }

    private static DocumentEnrichmentJobEntity pendingJob(JobType type, int attempts, int maxAttempts) {
        return new DocumentEnrichmentJobEntity(
                UUID.randomUUID(), UUID.randomUUID(), type, JobStatus.PENDING,
                attempts, maxAttempts, null, Instant.now(), Instant.now(), null, Instant.now(), Instant.now()
        );
    }

}
