package io.gnupinguin.nevis.wealthtech.persistence.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlQueryHelperTest {

    @Test
    void testReturnsEmptyBracketsForNullEmbedding() {
        var result = SqlQueryHelper.toVectorString(null);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    void testReturnsBracketedValuesForSingleElementEmbedding() {
        var result = SqlQueryHelper.toVectorString(new float[]{0.5f});

        assertThat(result).isEqualTo("[0.5]");
    }

    @Test
    void testFormatsMultipleElementEmbeddingAsCommaSeparatedList() {
        var result = SqlQueryHelper.toVectorString(new float[]{0.1f, 0.2f, 0.3f});

        assertThat(result).isEqualTo("[0.1, 0.2, 0.3]");
    }

    @Test
    void testReturnsEmptyBracketsForEmptyEmbedding() {
        var result = SqlQueryHelper.toVectorString(new float[]{});

        assertThat(result).isEqualTo("[]");
    }

}
