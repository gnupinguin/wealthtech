package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.concurrent.BoundedVirtualThreadExecutor;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import io.gnupinguin.nevis.wealthtech.service.enrichment.processor.DocumentEnrichmentJobProcessor;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentJobDispatcherTest {

    @Mock
    private DocumentEnrichmentJobRepository jobRepository;

    @Mock
    private BoundedVirtualThreadExecutor processorExecutor;

    @Mock
    private DocumentEnrichmentJobProcessor chunkingProcessor;

    @Mock
    private DocumentEnrichmentJobMetricsRecorder metricsRecorder;

    private DocumentEnrichmentJobRunner jobRunner;

    private DocumentEnrichmentJobDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(chunkingProcessor.type()).thenReturn(JobType.CHUNKING);
        lenient().when(processorExecutor.drainAvailableSlots()).thenReturn(2);
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(processorExecutor).executeReserved(any(Runnable.class));
        lenient().when(metricsRecorder.startSample()).thenReturn(Timer.start());
        jobRunner = new DocumentEnrichmentJobRunner(jobRepository, List.of(chunkingProcessor), metricsRecorder);
        dispatcher = new DocumentEnrichmentJobDispatcher(jobRepository, jobRunner, processorExecutor);
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
        verify(processorExecutor, times(2)).executeReserved(any(Runnable.class));
        verify(chunkingProcessor).process(firstJob);
        verify(chunkingProcessor).process(secondJob);
        verify(jobRepository, times(2)).save(any(DocumentEnrichmentJobEntity.class));
    }

    @Test
    void testDispatchNextJobsPullsOnlyFreeProcessorSlotCount() {
        doNothing().when(processorExecutor).executeReserved(any(Runnable.class));

        var job = pendingJob(JobType.CHUNKING, 0, 3);
        when(processorExecutor.drainAvailableSlots()).thenReturn(2, 1);
        when(jobRepository.tryLockNextPendingJobs(2)).thenReturn(List.of(job));
        when(jobRepository.tryLockNextPendingJobs(1)).thenReturn(List.of());

        dispatcher.dispatchNextJobs();
        dispatcher.dispatchNextJobs();

        verify(jobRepository).tryLockNextPendingJobs(2);
        verify(jobRepository).tryLockNextPendingJobs(1);
        verify(processorExecutor, times(1)).executeReserved(any(Runnable.class));
    }

    @Test
    void testDispatchNextJobsDoesNotReleaseSlotAgainWhenSubmissionFails() {
        var job = pendingJob(JobType.CHUNKING, 0, 3);
        when(processorExecutor.drainAvailableSlots()).thenReturn(1);
        when(jobRepository.tryLockNextPendingJobs(1)).thenReturn(List.of(job));
        doThrow(new RuntimeException("executor closed")).when(processorExecutor).executeReserved(any(Runnable.class));

        dispatcher.dispatchNextJobs();

        verify(processorExecutor).executeReserved(any(Runnable.class));
        verify(processorExecutor, never()).releaseSlots(anyInt());
        verify(chunkingProcessor, never()).process(any());
        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(JobStatus.PENDING);
        assertThat(captor.getValue().lastError()).isEqualTo("executor closed");
    }

    @Test
    void testDispatchNextJobsReleasesReservedSlotsWhenLockingFails() {
        when(jobRepository.tryLockNextPendingJobs(2)).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> dispatcher.dispatchNextJobs())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        verify(processorExecutor).releaseSlots(2);
        verify(processorExecutor, never()).executeReserved(any(Runnable.class));
    }

    private static DocumentEnrichmentJobEntity pendingJob(JobType type, int attempts, int maxAttempts) {
        return new DocumentEnrichmentJobEntity(
                UUID.randomUUID(), UUID.randomUUID(), type, JobStatus.PENDING,
                attempts, maxAttempts, null, Instant.now(), Instant.now(), null, Instant.now(), Instant.now()
        );
    }

}
