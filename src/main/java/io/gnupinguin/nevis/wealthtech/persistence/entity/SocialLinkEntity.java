package io.gnupinguin.nevis.wealthtech.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("client_social_links")
public record SocialLinkEntity(
        @Id UUID id,
        String url,
        Instant createdAt
) {}
