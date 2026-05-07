package io.gnupinguin.nevis.wealthtech.service.search.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClientQueryNormalizerTest {

    private final ClientQueryNormalizer normalizer = new ClientQueryNormalizer();

    @Test
    void testBasicQueryIsLowercasedAndPrefixed() {
        var result = normalizer.normalize("Hello");

        assertThat(result.query()).isEqualTo("hello");
        assertThat(result.prefix()).isEqualTo("hello%");
    }

    @Test
    void testLeadingAndTrailingWhitespaceIsTrimmed() {
        var result = normalizer.normalize("  Alice  ");

        assertThat(result.query()).isEqualTo("alice");
        assertThat(result.prefix()).isEqualTo("alice%");
    }

    @Test
    void testUppercaseLettersAreLowercased() {
        var result = normalizer.normalize("JOHN DOE");

        assertThat(result.query()).isEqualTo("john doe");
        assertThat(result.prefix()).isEqualTo("john doe%");
    }

    @Test
    void testPercentSignIsEscapedInPrefix() {
        var result = normalizer.normalize("50%");

        assertThat(result.query()).isEqualTo("50%");
        assertThat(result.prefix()).isEqualTo("50\\%%");
    }

    @Test
    void testUnderscoreIsEscapedInPrefix() {
        var result = normalizer.normalize("john_doe");

        assertThat(result.query()).isEqualTo("john_doe");
        assertThat(result.prefix()).isEqualTo("john\\_doe%");
    }

    @Test
    void testBackslashIsEscapedInPrefix() {
        var result = normalizer.normalize("foo\\bar");

        assertThat(result.query()).isEqualTo("foo\\bar");
        assertThat(result.prefix()).isEqualTo("foo\\\\bar%");
    }

    @Test
    void testMultipleSpecialCharsAreAllEscapedInPrefix() {
        var result = normalizer.normalize("a%b_c\\d");

        assertThat(result.query()).isEqualTo("a%b_c\\d");
        assertThat(result.prefix()).isEqualTo("a\\%b\\_c\\\\d%");
    }

    @Test
    void testEmptyStringReturnsEmptyQueryAndSinglePercentPrefix() {
        var result = normalizer.normalize("");

        assertThat(result.query()).isEmpty();
        assertThat(result.prefix()).isEqualTo("%");
    }

    @Test
    void testWhitespaceOnlyReturnsTrimmedEmptyQuery() {
        var result = normalizer.normalize("   ");

        assertThat(result.query()).isEmpty();
        assertThat(result.prefix()).isEqualTo("%");
    }

    @ParameterizedTest(name = "input=\"{0}\" -> query=\"{1}\", prefix=\"{2}\"")
    @CsvSource({
        "Smith,       smith,       smith%",
        "MÜLLER,      müller,      müller%",
        "  Bob  ,     bob,         bob%",
    })
    void testNormalizeVariousInputs(String input, String expectedQuery, String expectedPrefix) {
        var result = normalizer.normalize(input);

        assertThat(result.query()).isEqualTo(expectedQuery);
        assertThat(result.prefix()).isEqualTo(expectedPrefix);
    }

}
