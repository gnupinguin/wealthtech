package io.gnupinguin.nevis.wealthtech.service.search;

public record ScoredEntity<T>(T entity, float score) {
}
