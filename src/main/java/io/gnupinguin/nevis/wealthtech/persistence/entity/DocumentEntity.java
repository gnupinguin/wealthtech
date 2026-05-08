package io.gnupinguin.nevis.wealthtech.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("documents")
public record DocumentEntity(
        @Id UUID id,
        UUID clientId,
        String title,
        String content,
        String summary,
        Instant createdAt,
        Instant updatedAt
) {}
