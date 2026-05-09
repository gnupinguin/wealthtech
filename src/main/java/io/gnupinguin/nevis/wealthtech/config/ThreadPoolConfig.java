package io.gnupinguin.nevis.wealthtech.config;

import io.gnupinguin.nevis.wealthtech.concurrent.BoundedVirtualThreadExecutor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EnrichmentProperties.class, SearchProperties.class})
public class ThreadPoolConfig {

    @Bean("enrichmentProcessorExecutor")
    public BoundedVirtualThreadExecutor enrichmentProcessorExecutor(@NonNull EnrichmentProperties enrichmentProperties) {
        return new BoundedVirtualThreadExecutor("enrichment-processor-", enrichmentProperties.processor().poolSize());
    }

    @Bean("searchExecutor")
    public BoundedVirtualThreadExecutor searchExecutor(@NonNull SearchProperties searchProperties) {
        return new BoundedVirtualThreadExecutor("search-", searchProperties.poolSize());
    }

}
