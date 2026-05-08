package io.gnupinguin.nevis.wealthtech.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties({EnrichmentProperties.class, SearchProperties.class})
public class ThreadPoolConfig {

    @Bean("enrichmentProcessorExecutor")
    public ThreadPoolTaskExecutor enrichmentProcessorExecutor(EnrichmentProperties enrichmentProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(enrichmentProperties.processor().poolSize());
        executor.setMaxPoolSize(enrichmentProperties.processor().poolSize());
        executor.setThreadNamePrefix("enrichment-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean("searchExecutor")
    public ThreadPoolTaskExecutor searchExecutor(SearchProperties searchProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(searchProperties.poolSize());
        executor.setMaxPoolSize(searchProperties.poolSize());
        executor.setThreadNamePrefix("search-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

}
