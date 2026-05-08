package io.gnupinguin.nevis.wealthtech.service.search.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientQueryNormalizerTest {

    private final ClientQueryNormalizer normalizer = new ClientQueryNormalizer();

    @Test
    void testBasicQueryIsLowercasedAndPrefixed() {
        var result = normalizer.normalize("Hello");

        assertEquals("hello", result.query());
        assertEquals("hello%", result.prefix());
    }

    @Test
    void testLeadingAndTrailingWhitespaceIsTrimmed() {
        var result = normalizer.normalize("  Alice  ");

        assertEquals("alice", result.query());
        assertEquals("alice%", result.prefix());
    }

    @Test
    void testUppercaseLettersAreLowercased() {
        var result = normalizer.normalize("JOHN DOE");

        assertEquals("john doe", result.query());
        assertEquals("john doe%", result.prefix());
    }

    @Test
    void testPercentSignIsEscapedInPrefix() {
        var result = normalizer.normalize("50%");

        assertEquals("50%", result.query());
        assertEquals("50\\%%", result.prefix());
    }

    @Test
    void testUnderscoreIsEscapedInPrefix() {
        var result = normalizer.normalize("john_doe");

        assertEquals("john_doe", result.query());
        assertEquals("john\\_doe%", result.prefix());
    }

    @Test
    void testBackslashIsEscapedInPrefix() {
        var result = normalizer.normalize("foo\\bar");

        assertEquals("foo\\bar", result.query());
        assertEquals("foo\\\\bar%", result.prefix());
    }

    @Test
    void testMultipleSpecialCharsAreAllEscapedInPrefix() {
        var result = normalizer.normalize("a%b_c\\d");

        assertEquals("a%b_c\\d", result.query());
        assertEquals("a\\%b\\_c\\\\d%", result.prefix());
    }

    @Test
    void testEmptyStringReturnsEmptyQueryAndSinglePercentPrefix() {
        var result = normalizer.normalize("");

        assertThat(result.query()).isEmpty();
        assertEquals("%", result.prefix());
    }

    @Test
    void testWhitespaceOnlyReturnsTrimmedEmptyQuery() {
        var result = normalizer.normalize("   ");

        assertThat(result.query()).isEmpty();
        assertEquals("%", result.prefix());
    }

    @ParameterizedTest(name = "input=\"{0}\" -> query=\"{1}\", prefix=\"{2}\"")
    @CsvSource({
        "Smith,       smith,       smith%",
        "MÜLLER,      müller,      müller%",
        "  Bob  ,     bob,         bob%",
    })
    void testNormalizeVariousInputs(String input, String expectedQuery, String expectedPrefix) {
        var result = normalizer.normalize(input);

        assertEquals(expectedQuery, result.query());
        assertEquals(expectedPrefix, result.prefix());
    }

}
