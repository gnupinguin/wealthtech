package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEnrichmentProcessorMetricsTest {

    private ThreadPoolTaskExecutor processorExecutor;

    @BeforeEach
    void setUp() {
        processorExecutor = new ThreadPoolTaskExecutor();
        processorExecutor.setCorePoolSize(2);
        processorExecutor.setMaxPoolSize(2);
        processorExecutor.setQueueCapacity(4);
        processorExecutor.initialize();
    }

    @AfterEach
    void tearDown() {
        processorExecutor.shutdown();
    }

    @Test
    void testRegistersProcessorThreadPoolGauges() {
        var registry = new SimpleMeterRegistry();

        new DocumentEnrichmentProcessorMetrics(processorExecutor).bindTo(registry);

        assertThat(registry.find("document.enrichment.processor.jobs.running").gauge()).isNotNull();
        assertThat(registry.find("document.enrichment.processor.jobs.capacity").gauge()).isNotNull();
        assertThat(registry.find("document.enrichment.processor.pool.size").gauge()).isNotNull();
        assertThat(registry.find("document.enrichment.processor.pool.core.size").gauge()).isNotNull();
        assertThat(registry.find("document.enrichment.processor.queue.size").gauge()).isNotNull();
        assertThat(registry.find("document.enrichment.processor.queue.remaining.capacity").gauge()).isNotNull();
    }

    @Test
    void testProcessorThreadPoolGaugesReadExecutorState() {
        var registry = new SimpleMeterRegistry();

        new DocumentEnrichmentProcessorMetrics(processorExecutor).bindTo(registry);

        assertThat(gaugeValue(registry, "document.enrichment.processor.jobs.running")).isEqualTo(0.0);
        assertThat(gaugeValue(registry, "document.enrichment.processor.jobs.capacity")).isEqualTo(2.0);
        assertThat(gaugeValue(registry, "document.enrichment.processor.pool.core.size")).isEqualTo(2.0);
        assertThat(gaugeValue(registry, "document.enrichment.processor.queue.size")).isEqualTo(0.0);
        assertThat(gaugeValue(registry, "document.enrichment.processor.queue.remaining.capacity")).isEqualTo(4.0);
    }

    private static double gaugeValue(SimpleMeterRegistry registry, String name) {
        return registry.find(name).gauge().value();
    }

}
