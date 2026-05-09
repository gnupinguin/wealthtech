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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentJobDispatcherTest {

    @Mock
    private DocumentEnrichmentJobRepository jobRepository;

    @Mock
    private ThreadPoolTaskExecutor processorExecutor;

    @Mock
    private DocumentEnrichmentJobProcessor chunkingProcessor;

    private DocumentEnrichmentJobRunner jobRunner;

    private DocumentEnrichmentJobDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        var enrichmentProperties = new EnrichmentProperties(
                new EnrichmentProperties.Scheduler(5000L),
                new EnrichmentProperties.Processor(2)
        );
        when(chunkingProcessor.type()).thenReturn(JobType.CHUNKING);
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(processorExecutor).execute(any(Runnable.class));
        jobRunner = new DocumentEnrichmentJobRunner(jobRepository, List.of(chunkingProcessor));
        dispatcher = new DocumentEnrichmentJobDispatcher(jobRepository, jobRunner, processorExecutor, enrichmentProperties);
    }

    @Test
    void testDispatchNextJobsDoesNothingWhenNoJobsAvailable() {
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of());

        dispatcher.dispatchNextJobs();

        verify(jobRepository).tryLockNextPendingJobs(2);
        verify(jobRepository, never()).save(any());
        verify(chunkingProcessor, never()).process(any());
    }

    @Test
    void testDispatchNextJobsCompletesJobOnSuccessfulProcessing() {
        var job = pendingJob(JobType.CHUNKING, 0, 3);
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of(job));

        dispatcher.dispatchNextJobs();

        verify(chunkingProcessor).process(job);
        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(captor.getValue().completedAt()).isNotNull();
        assertThat(captor.getValue().lastError()).isNull();
    }

    @Test
    void testDispatchNextJobsSavesFailedJobWhenNoProcessorRegistered() {
        var job = pendingJob(JobType.SUMMARY, 0, 3);
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of(job));

        dispatcher.dispatchNextJobs();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.FAILED);
        assertThat(captor.getValue().lastError()).contains("No processor registered");
        verify(chunkingProcessor, never()).process(any());
    }

    @Test
    void testDispatchNextJobsRequeuesJobWithBackoffWhenProcessingFailsAndAttemptsRemain() {
        var job = pendingJob(JobType.CHUNKING, 1, 3);
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of(job));
        doThrow(new RuntimeException("processing error")).when(chunkingProcessor).process(job);

        dispatcher.dispatchNextJobs();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(JobStatus.PENDING);
        assertThat(saved.lastError()).isEqualTo("processing error");
        assertThat(saved.availableAt()).isAfter(Instant.now().minusSeconds(1));
        assertThat(saved.lockedAt()).isNull();
    }

    @Test
    void testDispatchNextJobsSavesFailedJobWhenMaxAttemptsReached() {
        var job = pendingJob(JobType.CHUNKING, 3, 3);
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of(job));
        doThrow(new RuntimeException("terminal error")).when(chunkingProcessor).process(job);

        dispatcher.dispatchNextJobs();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.FAILED);
        assertThat(captor.getValue().lastError()).isEqualTo("terminal error");
    }

    @Test
    void testDispatchNextJobsSubmitsUpToProcessorPoolSizeInOneTick() {
        var firstJob = pendingJob(JobType.CHUNKING, 0, 3);
        var secondJob = pendingJob(JobType.CHUNKING, 0, 3);
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of(firstJob, secondJob));

        dispatcher.dispatchNextJobs();

        verify(jobRepository).tryLockNextPendingJobs(2);
        verify(processorExecutor, times(2)).execute(any(Runnable.class));
        verify(chunkingProcessor).process(firstJob);
        verify(chunkingProcessor).process(secondJob);
        verify(jobRepository, times(2)).save(any(DocumentEnrichmentJobEntity.class));
    }

    @Test
    void testDispatchNextJobsPullsOnlyFreeProcessorSlotCount() {
        var enrichmentProperties = new EnrichmentProperties(
                new EnrichmentProperties.Scheduler(5000L),
                new EnrichmentProperties.Processor(2)
        );
        doNothing().when(processorExecutor).execute(any(Runnable.class));
        dispatcher = new DocumentEnrichmentJobDispatcher(jobRepository, jobRunner, processorExecutor, enrichmentProperties);

        var job = pendingJob(JobType.CHUNKING, 0, 3);
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of(job));
        when(jobRepository.tryLockNextPendingJobs(1)).thenReturn(List.of());

        dispatcher.dispatchNextJobs();
        dispatcher.dispatchNextJobs();

        verify(jobRepository).tryLockNextPendingJobs(2);
        verify(jobRepository).tryLockNextPendingJobs(1);
        verify(processorExecutor, times(1)).execute(any(Runnable.class));
    }

    private static DocumentEnrichmentJobEntity pendingJob(JobType type, int attempts, int maxAttempts) {
        return new DocumentEnrichmentJobEntity(
                UUID.randomUUID(), UUID.randomUUID(), type, JobStatus.PENDING,
                attempts, maxAttempts, null, Instant.now(), Instant.now(), null, Instant.now(), Instant.now()
        );
    }

}
