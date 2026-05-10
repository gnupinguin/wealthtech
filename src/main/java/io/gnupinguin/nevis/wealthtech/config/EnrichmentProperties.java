package io.gnupinguin.nevis.wealthtech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enrichment")
public record EnrichmentProperties(Scheduler scheduler, Kafka kafka, Summary summary) {

    private static final String DEFAULT_SUMMARY_MODEL = "gpt-5-nano";
    private static final int DEFAULT_SUMMARY_MAX_COMPLETION_TOKENS = 300;

    public EnrichmentProperties(Scheduler scheduler, Kafka kafka) {
        this(scheduler, kafka, new Summary(DEFAULT_SUMMARY_MODEL, DEFAULT_SUMMARY_MAX_COMPLETION_TOKENS));
    }

    public EnrichmentProperties {
        if (summary == null) {
            summary = new Summary(DEFAULT_SUMMARY_MODEL, DEFAULT_SUMMARY_MAX_COMPLETION_TOKENS);
        }
    }

    public record Scheduler(long fixedDelayMs, int batchSize) { }

    public record Summary(String model, int maxCompletionTokens) {

        public Summary {
            if (model == null || model.isBlank()) {
                model = DEFAULT_SUMMARY_MODEL;
            }
            if (maxCompletionTokens <= 0) {
                maxCompletionTokens = DEFAULT_SUMMARY_MAX_COMPLETION_TOKENS;
            }
        }

    }

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
