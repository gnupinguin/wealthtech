package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.entity.OutboxEventStatus;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentOutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DocumentEnrichmentOutboxMetrics implements MeterBinder {

    private static final String EVENTS_METRIC_NAME = "document.enrichment.outbox.events";

    private final DocumentEnrichmentOutboxEventRepository repository;

    public DocumentEnrichmentOutboxMetrics(@NonNull DocumentEnrichmentOutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        for (var status : OutboxEventStatus.values()) {
            for (var type : JobType.values()) {
                registerEventStateGauge(registry, status, type);
            }
        }
    }

    private void registerEventStateGauge(@NonNull MeterRegistry registry, @NonNull OutboxEventStatus status, @NonNull JobType type) {
        Gauge.builder(EVENTS_METRIC_NAME, repository, repo -> repo.countByStatusAndType(status, type))
                .description("Current number of document enrichment outbox events by status and type")
                .tag("status", tagValue(status.name()))
                .tag("type", tagValue(type.name()))
                .register(registry);
    }

    private static @NonNull String tagValue(@NonNull String value) {
        return value.toLowerCase(Locale.ROOT);
    }

}
