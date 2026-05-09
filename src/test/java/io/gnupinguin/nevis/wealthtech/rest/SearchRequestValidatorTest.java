package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.exception.BadRequestException;
import io.gnupinguin.nevis.wealthtech.rest.validation.DefaultSearchRequestValidator;
import io.gnupinguin.nevis.wealthtech.rest.validation.SearchRequestValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchRequestValidatorTest {

    private final SearchRequestValidator validator = new DefaultSearchRequestValidator();

    @Test
    void testAcceptsValidRequest() {
        assertDoesNotThrow(() -> validator.validate("abc", 1, 1));
        assertDoesNotThrow(() -> validator.validate("a".repeat(127), 20, 50));
    }

    @Test
    void testRejectsShortQuery() {
        assertBadRequest(() -> validator.validate("ab", 5, 10));
    }

    @Test
    void testRejectsLongQuery() {
        assertBadRequest(() -> validator.validate("a".repeat(128), 5, 10));
    }

    @Test
    void testRejectsClientLimitOutsideRange() {
        assertBadRequest(() -> validator.validate("growth", 0, 10));
        assertBadRequest(() -> validator.validate("growth", 21, 10));
    }

    @Test
    void testRejectsDocumentLimitOutsideRange() {
        assertBadRequest(() -> validator.validate("growth", 5, 0));
        assertBadRequest(() -> validator.validate("growth", 5, 51));
    }

    private static void assertBadRequest(Runnable request) {
        assertThrows(BadRequestException.class, request::run);
    }

}
