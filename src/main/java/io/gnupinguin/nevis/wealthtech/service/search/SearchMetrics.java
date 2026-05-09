package io.gnupinguin.nevis.wealthtech.service.search;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class SearchMetrics {

    private static final String SEARCH_TIMER_METRIC_NAME = "search.task.duration";
    private static final String TYPE_TAG = "type";
    private static final String OUTCOME_TAG = "outcome";
    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";

    private final Timer clientSearchSuccessTimer;
    private final Timer clientSearchFailureTimer;
    private final Timer documentSearchSuccessTimer;
    private final Timer documentSearchFailureTimer;

    public SearchMetrics(@NonNull MeterRegistry registry) {
        clientSearchSuccessTimer = buildTimer(registry, "client", OUTCOME_SUCCESS);
        clientSearchFailureTimer = buildTimer(registry, "client", OUTCOME_FAILURE);
        documentSearchSuccessTimer = buildTimer(registry, "document", OUTCOME_SUCCESS);
        documentSearchFailureTimer = buildTimer(registry, "document", OUTCOME_FAILURE);
    }

    public <T> T recordClientSearch(@NonNull Supplier<T> search) {
        return record(search, clientSearchSuccessTimer, clientSearchFailureTimer);
    }

    public <T> T recordDocumentSearch(@NonNull Supplier<T> search) {
        return record(search, documentSearchSuccessTimer, documentSearchFailureTimer);
    }

    private static <T> T record(@NonNull Supplier<T> search, @NonNull Timer successTimer, @NonNull Timer failureTimer) {
        Timer.Sample sample = Timer.start();
        try {
            T result = search.get();
            sample.stop(successTimer);
            return result;
        } catch (Exception e) {
            sample.stop(failureTimer);
            throw e;
        }
    }

    private static @NonNull Timer buildTimer(@NonNull MeterRegistry registry, @NonNull String type, @NonNull String outcome) {
        return Timer.builder(SEARCH_TIMER_METRIC_NAME)
                .description("Duration of individual search tasks by type and outcome")
                .tag(TYPE_TAG, type)
                .tag(OUTCOME_TAG, outcome)
                .register(registry);
    }

}
