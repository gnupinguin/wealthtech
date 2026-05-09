package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DocumentEnrichmentJobMetrics implements MeterBinder {

    private static final String JOBS_METRIC_NAME = "document.enrichment.jobs";

    private final DocumentEnrichmentJobRepository jobRepository;

    public DocumentEnrichmentJobMetrics(@NonNull DocumentEnrichmentJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        for (var status : JobStatus.values()) {
            for (var type : JobType.values()) {
                registerJobStateGauge(registry, status, type);
            }
        }
    }

    private void registerJobStateGauge(@NonNull MeterRegistry registry, @NonNull JobStatus status, @NonNull JobType type) {
        Gauge.builder(JOBS_METRIC_NAME, jobRepository, repository -> repository.countByStatusAndType(status, type))
                .description("Current number of document enrichment jobs by status and type")
                .tag("status", tagValue(status.name()))
                .tag("type", tagValue(type.name()))
                .register(registry);
    }

    private static @NonNull String tagValue(@NonNull String value) {
        return value.toLowerCase(Locale.ROOT);
    }

}
