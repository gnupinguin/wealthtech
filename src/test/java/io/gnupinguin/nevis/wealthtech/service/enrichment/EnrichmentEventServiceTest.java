package io.gnupinguin.nevis.wealthtech.service.enrichment;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrichmentEventServiceTest {

    @Mock
    private DocumentEnrichmentOutboxEventRepository repository;

    private EnrichmentEventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EnrichmentEventService(repository);
    }

    @Test
    void testEnqueueEventStoresPendingOutboxEvent() {
        var documentId = UUID.randomUUID();

        eventService.enqueueEvent(documentId, JobType.CHUNKING);

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentOutboxEventEntity.class);
        verify(repository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.id()).isNull();
        assertThat(saved.documentId()).isEqualTo(documentId);
        assertThat(saved.type()).isEqualTo(JobType.CHUNKING);
        assertThat(saved.status()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(saved.attempts()).isZero();
        assertThat(saved.availableAt()).isNotNull();
        assertThat(saved.lockedAt()).isNull();
        assertThat(saved.publishedAt()).isNull();
    }

}
