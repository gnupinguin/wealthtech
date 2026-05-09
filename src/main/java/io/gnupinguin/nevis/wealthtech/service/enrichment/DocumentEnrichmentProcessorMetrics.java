package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class DocumentEnrichmentProcessorMetrics implements MeterBinder {

    private static final String RUNNING_JOBS_METRIC_NAME = "document.enrichment.processor.jobs.running";
    private static final String JOB_CAPACITY_METRIC_NAME = "document.enrichment.processor.jobs.capacity";
    private static final String POOL_SIZE_METRIC_NAME = "document.enrichment.processor.pool.size";
    private static final String CORE_POOL_SIZE_METRIC_NAME = "document.enrichment.processor.pool.core.size";
    private static final String QUEUE_SIZE_METRIC_NAME = "document.enrichment.processor.queue.size";
    private static final String QUEUE_REMAINING_CAPACITY_METRIC_NAME = "document.enrichment.processor.queue.remaining.capacity";

    private final ThreadPoolTaskExecutor processorExecutor;

    public DocumentEnrichmentProcessorMetrics(
            @Qualifier("enrichmentProcessorExecutor") @NonNull ThreadPoolTaskExecutor processorExecutor) {
        this.processorExecutor = processorExecutor;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Gauge.builder(RUNNING_JOBS_METRIC_NAME, processorExecutor, ThreadPoolTaskExecutor::getActiveCount)
                .description("Current number of running document enrichment processor jobs")
                .register(registry);

        Gauge.builder(JOB_CAPACITY_METRIC_NAME, processorExecutor, ThreadPoolTaskExecutor::getMaxPoolSize)
                .description("Maximum number of document enrichment processor jobs that can run concurrently")
                .register(registry);

        Gauge.builder(POOL_SIZE_METRIC_NAME, processorExecutor, ThreadPoolTaskExecutor::getPoolSize)
                .description("Current document enrichment processor thread pool size")
                .register(registry);

        Gauge.builder(CORE_POOL_SIZE_METRIC_NAME, processorExecutor, ThreadPoolTaskExecutor::getCorePoolSize)
                .description("Configured core document enrichment processor thread pool size")
                .register(registry);

        Gauge.builder(QUEUE_SIZE_METRIC_NAME, processorExecutor, DocumentEnrichmentProcessorMetrics::queueSize)
                .description("Current number of queued document enrichment processor jobs")
                .register(registry);

        Gauge.builder(QUEUE_REMAINING_CAPACITY_METRIC_NAME, processorExecutor, DocumentEnrichmentProcessorMetrics::queueRemainingCapacity)
                .description("Remaining document enrichment processor queue capacity")
                .register(registry);
    }

    private static int queueSize(@NonNull ThreadPoolTaskExecutor executor) {
        return executor.getThreadPoolExecutor().getQueue().size();
    }

    private static int queueRemainingCapacity(@NonNull ThreadPoolTaskExecutor executor) {
        return executor.getThreadPoolExecutor().getQueue().remainingCapacity();
    }

}
