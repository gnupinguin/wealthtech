package io.gnupinguin.nevis.wealthtech.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Table("clients")
public record ClientEntity(
        @Id UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        Instant createdAt,
        @MappedCollection(idColumn = "client_id") Set<SocialLinkEntity> socialLinks
) {}
