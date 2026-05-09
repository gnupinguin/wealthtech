package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.concurrent.BoundedVirtualThreadExecutor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SearchExecutorMetrics implements MeterBinder {

    private static final String RUNNING_TASKS_METRIC_NAME = "search.executor.tasks.running";
    private static final String TASK_CAPACITY_METRIC_NAME = "search.executor.tasks.capacity";
    private static final String AVAILABLE_TASKS_METRIC_NAME = "search.executor.tasks.available";

    private final BoundedVirtualThreadExecutor searchExecutor;

    public SearchExecutorMetrics(@Qualifier("searchExecutor") @NonNull BoundedVirtualThreadExecutor searchExecutor) {
        this.searchExecutor = searchExecutor;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Gauge.builder(RUNNING_TASKS_METRIC_NAME, searchExecutor, BoundedVirtualThreadExecutor::runningTasks)
                .description("Current number of running search tasks")
                .register(registry);

        Gauge.builder(TASK_CAPACITY_METRIC_NAME, searchExecutor, BoundedVirtualThreadExecutor::capacity)
                .description("Maximum number of search tasks that can run concurrently")
                .register(registry);

        Gauge.builder(AVAILABLE_TASKS_METRIC_NAME, searchExecutor, BoundedVirtualThreadExecutor::availableSlots)
                .description("Current number of available search task slots")
                .register(registry);
    }

}
