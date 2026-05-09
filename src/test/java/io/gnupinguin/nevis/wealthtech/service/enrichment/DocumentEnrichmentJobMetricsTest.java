package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentJobMetricsTest {

    @Mock
    private DocumentEnrichmentJobRepository jobRepository;

    @Test
    void testRegistersJobStateGaugeForEachStatusAndType() {
        var registry = new SimpleMeterRegistry();

        new DocumentEnrichmentJobMetrics(jobRepository).bindTo(registry);

        assertThat(registry.find("document.enrichment.jobs").gauges())
                .hasSize(JobStatus.values().length * JobType.values().length);
    }

    @Test
    void testJobStateGaugeReadsCurrentRepositoryCount() {
        when(jobRepository.countByStatusAndType(JobStatus.PENDING, JobType.CHUNKING)).thenReturn(3L);
        var registry = new SimpleMeterRegistry();

        new DocumentEnrichmentJobMetrics(jobRepository).bindTo(registry);

        var gauge = registry.find("document.enrichment.jobs")
                .tag("status", "pending")
                .tag("type", "chunking")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(3.0);
    }

}
