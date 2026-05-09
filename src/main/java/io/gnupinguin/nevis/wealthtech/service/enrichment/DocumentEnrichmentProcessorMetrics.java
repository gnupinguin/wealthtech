package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.concurrent.BoundedVirtualThreadExecutor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DocumentEnrichmentProcessorMetrics implements MeterBinder {

    private static final String RUNNING_JOBS_METRIC_NAME = "document.enrichment.processor.jobs.running";
    private static final String JOB_CAPACITY_METRIC_NAME = "document.enrichment.processor.jobs.capacity";
    private static final String AVAILABLE_JOBS_METRIC_NAME = "document.enrichment.processor.jobs.available";

    private final BoundedVirtualThreadExecutor processorExecutor;

    public DocumentEnrichmentProcessorMetrics(
            @Qualifier("enrichmentProcessorExecutor") @NonNull BoundedVirtualThreadExecutor processorExecutor) {
        this.processorExecutor = processorExecutor;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Gauge.builder(RUNNING_JOBS_METRIC_NAME, processorExecutor, BoundedVirtualThreadExecutor::runningTasks)
                .description("Current number of running document enrichment processor jobs")
                .register(registry);

        Gauge.builder(JOB_CAPACITY_METRIC_NAME, processorExecutor, BoundedVirtualThreadExecutor::capacity)
                .description("Maximum number of document enrichment processor jobs that can run concurrently")
                .register(registry);

        Gauge.builder(AVAILABLE_JOBS_METRIC_NAME, processorExecutor, BoundedVirtualThreadExecutor::availableSlots)
                .description("Current number of available document enrichment processor job slots")
                .register(registry);
    }

}
