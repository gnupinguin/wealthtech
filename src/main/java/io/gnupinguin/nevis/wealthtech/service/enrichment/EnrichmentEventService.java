package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentOutboxEventEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.entity.OutboxEventStatus;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentEventService {

    private final DocumentEnrichmentOutboxEventRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueEvent(@NonNull UUID documentId, @NonNull JobType type) {
        var now = Instant.now();
        repository.save(new DocumentEnrichmentOutboxEventEntity(
                null,
                documentId,
                type,
                OutboxEventStatus.PENDING,
                0,
                null,
                now,
                null,
                null,
                now,
                now
        ));
    }

}
