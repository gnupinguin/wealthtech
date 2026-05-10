package io.gnupinguin.nevis.wealthtech.config;

import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(@NonNull Environment environment) {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers(environment)));
    }

    @Bean
    public ProducerFactory<String, DocumentEnrichmentEvent> documentEnrichmentProducerFactory(@NonNull Environment environment) {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers(environment));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public DefaultKafkaConsumerFactory<String, DocumentEnrichmentEvent> documentEnrichmentConsumerFactory(@NonNull Environment environment) {
        var props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers(environment));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty(
                "spring.kafka.consumer.group-id",
                "wealthtech-document-enrichment"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, environment.getProperty(
                "spring.kafka.consumer.auto-offset-reset",
                "earliest"));
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "io.gnupinguin.nevis.wealthtech.service.enrichment");
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, DocumentEnrichmentEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, DocumentEnrichmentEvent> documentEnrichmentKafkaTemplate(
            @NonNull ProducerFactory<String, DocumentEnrichmentEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic documentEnrichmentTopic(@NonNull EnrichmentProperties enrichmentProperties) {
        return TopicBuilder.name(enrichmentProperties.kafka().topic())
                .partitions(enrichmentProperties.kafka().partitions())
                .replicas(enrichmentProperties.kafka().replicationFactor())
                .build();
    }

    @Bean
    public NewTopic documentEnrichmentDeadLetterTopic(@NonNull EnrichmentProperties enrichmentProperties) {
        return TopicBuilder.name(enrichmentProperties.kafka().deadLetterTopic())
                .partitions(enrichmentProperties.kafka().partitions())
                .replicas(enrichmentProperties.kafka().replicationFactor())
                .build();
    }

    @Bean
    public DefaultErrorHandler documentEnrichmentErrorHandler(
            @NonNull KafkaTemplate<String, DocumentEnrichmentEvent> kafkaTemplate,
            @NonNull EnrichmentProperties enrichmentProperties) {
        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, _) -> new TopicPartition(enrichmentProperties.kafka().deadLetterTopic(), record.partition()));
        var retryAttempts = Math.max(0L, enrichmentProperties.kafka().consumer().retryAttempts() - 1L);
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(enrichmentProperties.kafka().consumer().retryBackoffMs(), retryAttempts));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentEnrichmentEvent> documentEnrichmentKafkaListenerContainerFactory(
            @NonNull DefaultKafkaConsumerFactory<String, DocumentEnrichmentEvent> documentEnrichmentConsumerFactory,
            @NonNull DefaultErrorHandler documentEnrichmentErrorHandler,
            @NonNull EnrichmentProperties enrichmentProperties) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, DocumentEnrichmentEvent>();
        factory.setConsumerFactory(documentEnrichmentConsumerFactory);
        factory.setCommonErrorHandler(documentEnrichmentErrorHandler);
        factory.setConcurrency(enrichmentProperties.kafka().consumer().concurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    private static @NonNull String bootstrapServers(@NonNull Environment environment) {
        return environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
    }

}
