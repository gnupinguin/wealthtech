package io.gnupinguin.nevis.wealthtech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search")
public record SearchProperties(
        int poolSize,
        long clientTimeoutMs,
        long documentTimeoutMs
) {
}
