package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentProcessedEventRepository;
import io.gnupinguin.nevis.wealthtech.service.enrichment.processor.DocumentEnrichmentProcessor;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DocumentEnrichmentEventProcessor {

    private final DocumentEnrichmentProcessedEventRepository processedEventRepository;
    private final Map<JobType, DocumentEnrichmentProcessor> processors;
    private final DocumentEnrichmentEventMetricsRecorder metricsRecorder;

    public DocumentEnrichmentEventProcessor(
            @NonNull DocumentEnrichmentProcessedEventRepository processedEventRepository,
            @NonNull List<DocumentEnrichmentProcessor> processors,
            @NonNull DocumentEnrichmentEventMetricsRecorder metricsRecorder) {
        this.processedEventRepository = processedEventRepository;
        this.processors = processors.stream()
                .collect(Collectors.toMap(DocumentEnrichmentProcessor::type, Function.identity()));
        this.metricsRecorder = metricsRecorder;
    }

    @Transactional
    public void process(@NonNull DocumentEnrichmentEvent event) {
        Sample sample = metricsRecorder.startSample();
        if (!processedEventRepository.recordProcessingStarted(event)) {
            log.info("Skipping duplicate document enrichment event {} (type={}, document={})",
                    event.id(), event.type(), event.documentId());
            metricsRecorder.recordDuplicate(sample, event.type());
            return;
        }

        var processor = processors.get(event.type());
        if (processor == null) {
            metricsRecorder.recordFailed(sample, event.type());
            throw new IllegalStateException("No processor registered for type: " + event.type());
        }

        try {
            processor.process(event);
            metricsRecorder.recordCompleted(sample, event.type());
            log.info("Document enrichment event {} (type={}, document={}) completed successfully",
                    event.id(), event.type(), event.documentId());
        } catch (Exception e) {
            metricsRecorder.recordFailed(sample, event.type());
            throw e;
        }
    }

}
