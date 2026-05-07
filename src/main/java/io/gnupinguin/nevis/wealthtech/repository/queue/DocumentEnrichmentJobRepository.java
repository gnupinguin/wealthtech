package io.gnupinguin.nevis.wealthtech.repository.queue;

import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface DocumentEnrichmentJobRepository extends CrudRepository<DocumentEnrichmentJobEntity, UUID> {}
