package io.gnupinguin.nevis.wealthtech.service.enrichment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEnrichmentEventListener {

    private final DocumentEnrichmentEventProcessor processor;

    @KafkaListener(
            topics = "${enrichment.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "documentEnrichmentKafkaListenerContainerFactory")
    public void onEvent(@NonNull DocumentEnrichmentEvent event) {
        log.info("Received document enrichment event {} (type={}, document={})",
                event.id(), event.type(), event.documentId());
        processor.process(event);
    }

}
