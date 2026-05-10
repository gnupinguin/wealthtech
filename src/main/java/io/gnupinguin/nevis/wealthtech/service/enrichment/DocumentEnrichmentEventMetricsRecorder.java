package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

@Component
public class DocumentEnrichmentEventMetricsRecorder {

    private static final String EVENT_DURATION_METRIC_NAME = "document.enrichment.event.duration";
    private static final String TYPE_TAG = "type";
    private static final String OUTCOME_TAG = "outcome";
    static final String OUTCOME_COMPLETED = "completed";
    static final String OUTCOME_DUPLICATE = "duplicate";
    static final String OUTCOME_FAILED = "failed";

    private final Map<JobType, Timer> completedTimers = new EnumMap<>(JobType.class);
    private final Map<JobType, Timer> duplicateTimers = new EnumMap<>(JobType.class);
    private final Map<JobType, Timer> failedTimers = new EnumMap<>(JobType.class);

    public DocumentEnrichmentEventMetricsRecorder(@NonNull MeterRegistry registry) {
        for (var type : JobType.values()) {
            completedTimers.put(type, buildTimer(registry, type, OUTCOME_COMPLETED));
            duplicateTimers.put(type, buildTimer(registry, type, OUTCOME_DUPLICATE));
            failedTimers.put(type, buildTimer(registry, type, OUTCOME_FAILED));
        }
    }

    public @NonNull Sample startSample() {
        return Timer.start();
    }

    public void recordCompleted(@NonNull Sample sample, @NonNull JobType type) {
        sample.stop(completedTimers.get(type));
    }

    public void recordDuplicate(@NonNull Sample sample, @NonNull JobType type) {
        sample.stop(duplicateTimers.get(type));
    }

    public void recordFailed(@NonNull Sample sample, @NonNull JobType type) {
        sample.stop(failedTimers.get(type));
    }

    private static @NonNull Timer buildTimer(@NonNull MeterRegistry registry, @NonNull JobType type, @NonNull String outcome) {
        return Timer.builder(EVENT_DURATION_METRIC_NAME)
                .description("Duration of document enrichment event processing by type and outcome")
                .tag(TYPE_TAG, type.name().toLowerCase(Locale.ROOT))
                .tag(OUTCOME_TAG, outcome)
                .register(registry);
    }

}
