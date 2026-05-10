package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.config.EnrichmentProperties;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentOutboxEventEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.entity.OutboxEventStatus;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentOutboxPublisherTest {

    @Mock
    private DocumentEnrichmentOutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, DocumentEnrichmentEvent> kafkaTemplate;

    private EnrichmentProperties properties;
    private DocumentEnrichmentOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new EnrichmentProperties(
                new EnrichmentProperties.Scheduler(1000, 10),
                new EnrichmentProperties.Kafka(
                        "document-enrichment-events",
                        "document-enrichment-events-dlt",
                        8,
                        (short) 1,
                        new EnrichmentProperties.Producer(5000, 600000),
                        new EnrichmentProperties.Consumer(4, 3, 1000)
                ),
                null
        );
        publisher = new DocumentEnrichmentOutboxPublisher(repository, kafkaTemplate, properties);
    }

    @Test
    void testPublishPendingEventsUsesDocumentIdAsKafkaKeyAndMarksPublished() {
        var event = outboxEvent(JobType.CHUNKING);
        when(repository.lockNextPublishableEvents(10, 600000)).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("document-enrichment-events"), eq(event.documentId().toString()), any(DocumentEnrichmentEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentEvent.class);
        verify(kafkaTemplate).send(eq("document-enrichment-events"), eq(event.documentId().toString()), captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(event.id());
        assertThat(captor.getValue().documentId()).isEqualTo(event.documentId());
        assertThat(captor.getValue().type()).isEqualTo(JobType.CHUNKING);
        verify(repository).markPublished(event.id());
    }

    @Test
    void testPublishPendingEventsRequeuesOutboxEventWhenKafkaSendFails() {
        var event = outboxEvent(JobType.SUMMARY);
        when(repository.lockNextPublishableEvents(10, 600000)).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any(DocumentEnrichmentEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        publisher.publishPendingEvents();

        verify(repository).markPublishFailed(eq(event.id()), eq("kafka down"), any(Instant.class));
    }

    private static DocumentEnrichmentOutboxEventEntity outboxEvent(JobType type) {
        var now = Instant.now();
        return new DocumentEnrichmentOutboxEventEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                OutboxEventStatus.PROCESSING,
                1,
                null,
                now,
                now,
                null,
                now,
                now
        );
    }

}
