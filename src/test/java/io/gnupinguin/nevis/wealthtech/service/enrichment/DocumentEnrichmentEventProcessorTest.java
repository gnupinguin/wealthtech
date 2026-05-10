package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentProcessedEventRepository;
import io.gnupinguin.nevis.wealthtech.service.enrichment.processor.DocumentEnrichmentProcessor;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentEventProcessorTest {

    @Mock
    private DocumentEnrichmentProcessedEventRepository processedEventRepository;

    @Mock
    private DocumentEnrichmentProcessor chunkingProcessor;

    @Mock
    private DocumentEnrichmentEventMetricsRecorder metricsRecorder;

    private DocumentEnrichmentEventProcessor eventProcessor;

    @BeforeEach
    void setUp() {
        when(chunkingProcessor.type()).thenReturn(JobType.CHUNKING);
        lenient().when(metricsRecorder.startSample()).thenReturn(Timer.start());
        eventProcessor = new DocumentEnrichmentEventProcessor(
                processedEventRepository,
                List.of(chunkingProcessor),
                metricsRecorder
        );
    }

    @Test
    void testProcessRunsProcessorForNewEvent() {
        var event = event(JobType.CHUNKING);
        when(processedEventRepository.recordProcessingStarted(event)).thenReturn(true);

        eventProcessor.process(event);

        verify(chunkingProcessor).process(event);
        verify(metricsRecorder).recordCompleted(any(), eq(JobType.CHUNKING));
    }

    @Test
    void testProcessSkipsDuplicateEvent() {
        var event = event(JobType.CHUNKING);
        when(processedEventRepository.recordProcessingStarted(event)).thenReturn(false);

        eventProcessor.process(event);

        verify(chunkingProcessor, never()).process(any());
        verify(metricsRecorder).recordDuplicate(any(), eq(JobType.CHUNKING));
    }

    @Test
    void testProcessRethrowsFailureSoKafkaErrorHandlerCanSendToDeadLetterTopic() {
        var event = event(JobType.CHUNKING);
        when(processedEventRepository.recordProcessingStarted(event)).thenReturn(true);
        doThrow(new RuntimeException("processor failed")).when(chunkingProcessor).process(event);

        assertThatThrownBy(() -> eventProcessor.process(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("processor failed");

        verify(metricsRecorder).recordFailed(any(), eq(JobType.CHUNKING));
    }

    private static DocumentEnrichmentEvent event(JobType type) {
        return new DocumentEnrichmentEvent(UUID.randomUUID(), UUID.randomUUID(), type, Instant.now());
    }

}
