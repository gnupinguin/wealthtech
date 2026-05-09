package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.concurrent.BoundedVirtualThreadExecutor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEnrichmentProcessorMetricsTest {

    @Test
    void testRegistersProcessorSlotGauges() {
        var registry = new SimpleMeterRegistry();
        var executor = new BoundedVirtualThreadExecutor("test-", 2);

        new DocumentEnrichmentProcessorMetrics(executor).bindTo(registry);

        assertThat(registry.find("document.enrichment.processor.jobs.running").gauge()).isNotNull();
        assertThat(registry.find("document.enrichment.processor.jobs.capacity").gauge()).isNotNull();
        assertThat(registry.find("document.enrichment.processor.jobs.available").gauge()).isNotNull();
    }

    @Test
    void testProcessorSlotGaugesReadExecutorState() {
        var registry = new SimpleMeterRegistry();
        var executor = new BoundedVirtualThreadExecutor("test-", 2);

        new DocumentEnrichmentProcessorMetrics(executor).bindTo(registry);

        assertThat(gaugeValue(registry, "document.enrichment.processor.jobs.running")).isEqualTo(0.0);
        assertThat(gaugeValue(registry, "document.enrichment.processor.jobs.capacity")).isEqualTo(2.0);
        assertThat(gaugeValue(registry, "document.enrichment.processor.jobs.available")).isEqualTo(2.0);
    }

    @Test
    void testProcessorSlotGaugesReflectSubmittedRunningJobs() throws InterruptedException {
        var registry = new SimpleMeterRegistry();
        var executor = new BoundedVirtualThreadExecutor("test-", 2);
        var jobStarted = new CountDownLatch(1);
        var releaseJob = new CountDownLatch(1);

        new DocumentEnrichmentProcessorMetrics(executor).bindTo(registry);
        executor.execute(() -> {
            jobStarted.countDown();
            await(releaseJob);
        });
        assertThat(jobStarted.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        try {
            assertThat(gaugeValue(registry, "document.enrichment.processor.jobs.running")).isEqualTo(1.0);
            assertThat(gaugeValue(registry, "document.enrichment.processor.jobs.available")).isEqualTo(1.0);
        } finally {
            releaseJob.countDown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static double gaugeValue(SimpleMeterRegistry registry, String name) {
        return registry.find(name).gauge().value();
    }

}
