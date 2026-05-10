package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.entity.OutboxEventStatus;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentOutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentOutboxMetricsTest {

    @Mock
    private DocumentEnrichmentOutboxEventRepository repository;

    @Test
    void testRegistersGaugeForEveryOutboxStatusAndType() {
        var registry = new SimpleMeterRegistry();

        new DocumentEnrichmentOutboxMetrics(repository).bindTo(registry);

        assertThat(registry.find("document.enrichment.outbox.events").gauges())
                .hasSize(OutboxEventStatus.values().length * JobType.values().length);
    }

    @Test
    void testGaugeReadsRepositoryCount() {
        when(repository.countByStatusAndType(OutboxEventStatus.PENDING, JobType.CHUNKING)).thenReturn(3L);
        var registry = new SimpleMeterRegistry();

        new DocumentEnrichmentOutboxMetrics(repository).bindTo(registry);

        var gauge = registry.find("document.enrichment.outbox.events")
                .tag("status", "pending")
                .tag("type", "chunking")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(3.0);
    }

}
