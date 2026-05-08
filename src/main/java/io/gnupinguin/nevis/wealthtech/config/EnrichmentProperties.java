package io.gnupinguin.nevis.wealthtech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enrichment")
public record EnrichmentProperties(Scheduler scheduler, Processor processor) {

    public record Scheduler(long fixedDelayMs) { }

    public record Processor(int poolSize, long timeoutMs) { }

}
