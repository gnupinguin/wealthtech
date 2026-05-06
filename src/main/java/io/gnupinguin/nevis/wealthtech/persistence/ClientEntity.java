package io.gnupinguin.nevis.wealthtech.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@Table("clients")
public record ClientEntity(
        @Id UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        @MappedCollection(idColumn = "client_id", keyColumn = "position") List<SocialLink> socialLinks
) {}
