package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchEntity;

import java.util.List;

public record SearchResult(List<ScoredEntity<ClientEntity>> clients, List<DocumentSearchEntity> documents) {
}
