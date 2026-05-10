package io.gnupinguin.nevis.wealthtech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enrichment")
public record EnrichmentProperties(Scheduler scheduler, Kafka kafka) {

    public record Scheduler(long fixedDelayMs, int batchSize) { }

    public record Kafka(
            String topic,
            String deadLetterTopic,
            int partitions,
            short replicationFactor,
            Producer producer,
            Consumer consumer) { }

    public record Producer(long sendTimeoutMs, long lockTimeoutMs) { }

    public record Consumer(int concurrency, long retryAttempts, long retryBackoffMs) { }

}
