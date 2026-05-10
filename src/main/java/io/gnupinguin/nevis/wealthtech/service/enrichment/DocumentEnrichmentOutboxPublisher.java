package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.config.EnrichmentProperties;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentOutboxEventEntity;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEnrichmentOutboxPublisher {

    private final DocumentEnrichmentOutboxEventRepository repository;
    private final KafkaTemplate<String, DocumentEnrichmentEvent> kafkaTemplate;
    private final EnrichmentProperties enrichmentProperties;

    @Scheduled(fixedDelayString = "${enrichment.scheduler.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        var events = repository.lockNextPublishableEvents(
                enrichmentProperties.scheduler().batchSize(),
                enrichmentProperties.kafka().producer().lockTimeoutMs());

        for (var event : events) {
            publish(event);
        }
    }

    private void publish(@NonNull DocumentEnrichmentOutboxEventEntity event) {
        var payload = new DocumentEnrichmentEvent(event.id(), event.documentId(), event.type(), event.createdAt());
        try {
            kafkaTemplate
                    .send(enrichmentProperties.kafka().topic(), event.documentId().toString(), payload)
                    .get(enrichmentProperties.kafka().producer().sendTimeoutMs(), TimeUnit.MILLISECONDS);

            repository.markPublished(event.id());
            log.info("Published document enrichment event {} (type={}, document={})",
                    event.id(), event.type(), event.documentId());
        } catch (Exception e) {
            var error = errorMessage(e);
            repository.markPublishFailed(event.id(), error, nextAvailableAt(event.attempts()));
            log.warn("Could not publish document enrichment event {} (type={}, document={}): {}",
                    event.id(), event.type(), event.documentId(), error);
        }
    }

    private static @NonNull Instant nextAvailableAt(int attempts) {
        var backoffSeconds = Math.min(300L, Math.max(1L, attempts) * 10L);
        return Instant.now().plusSeconds(backoffSeconds);
    }

    private static @NonNull String errorMessage(@NonNull Throwable e) {
        var cause = e.getCause() == null ? e : e.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

}
