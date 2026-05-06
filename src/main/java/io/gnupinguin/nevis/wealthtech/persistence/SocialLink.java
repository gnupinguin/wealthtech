package io.gnupinguin.nevis.wealthtech.persistence;

import org.springframework.data.relational.core.mapping.Table;

@Table("client_social_links")
public record SocialLink(String value) {}
