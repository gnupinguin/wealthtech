package io.gnupinguin.nevis.wealthtech.persistence.repository;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface DocumentRepository extends CrudRepository<DocumentEntity, UUID> {}
