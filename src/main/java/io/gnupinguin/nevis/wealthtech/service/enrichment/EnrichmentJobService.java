package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import io.gnupinguin.nevis.wealthtech.repository.DocumentEnrichmentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentJobService implements JobService {

    private final DocumentEnrichmentJobRepository repository;

    @Override
    public void publishJob(@NonNull UUID documentId, @NonNull JobType type) {
        var now = Instant.now();
        var job = getDocumentEnrichmentJobEntity(documentId, type, now);
        repository.save(job);
    }

    private static @NonNull DocumentEnrichmentJobEntity getDocumentEnrichmentJobEntity(@NonNull UUID documentId, @NonNull JobType type, @NonNull Instant now) {
        return new DocumentEnrichmentJobEntity(
                null, documentId, type, JobStatus.PENDING,
                0, 3, null, now, null, null, now, now);
    }

}
