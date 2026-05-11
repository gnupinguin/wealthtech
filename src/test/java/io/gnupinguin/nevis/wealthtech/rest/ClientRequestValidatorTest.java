package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.exception.BadRequestException;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.rest.validation.ClientRequestValidator;
import io.gnupinguin.nevis.wealthtech.rest.validation.DefaultClientRequestValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientRequestValidatorTest {

    private final ClientRequestValidator validator = new DefaultClientRequestValidator();

    @Test
    void testAcceptsValidClientRequest() {
        var request = new CreateClientRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                "Technology investor",
                List.of("https://example.com/ada")
        );

        assertDoesNotThrow(() -> validator.validateCreateClient(request));
    }

    @Test
    void testRejectsClientRequestWithMissingRequiredFields() {
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                null, "Lovelace", "ada@example.com", null, null
        )), "first_name is required");
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "Ada", " ", "ada@example.com", null, null
        )), "last_name is required");
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "Ada", "Lovelace", null, null, null
        )), "email is required");
    }

    @Test
    void testRejectsClientRequestWithInvalidEmail() {
        var request = new CreateClientRequest("Ada", "Lovelace", "not-email", null, null);

        assertBadRequest(() -> validator.validateCreateClient(request), "email must be a valid email address");
    }

    @Test
    void testRejectsClientRequestWithTooLongFields() {
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "x".repeat(101), "Lovelace", "ada@example.com", null, null
        )), "first_name must be at most 100 characters");
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "Ada", "x".repeat(101), "ada@example.com", null, null
        )), "last_name must be at most 100 characters");
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "Ada", "Lovelace", "x".repeat(309) + "@example.com", null, null
        )), "email must be at most 320 characters");
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "Ada", "Lovelace", "ada@example.com", "x".repeat(4097), null
        )), "description must be at most 4096 characters");
    }

    @Test
    void testRejectsClientRequestWithBlankSocialLink() {
        var request = new CreateClientRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                null,
                List.of(" ")
        );

        assertBadRequest(() -> validator.validateCreateClient(request), "social_links entries must be non-blank");
    }

    @Test
    void testRejectsClientRequestWithTooLongSocialLink() {
        var request = new CreateClientRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                null,
                List.of("x".repeat(2049))
        );

        assertBadRequest(() -> validator.validateCreateClient(request), "social_links entries must be at most 2048 characters");
    }

    @Test
    void testAcceptsValidDocumentRequest() {
        var request = new CreateDocumentRequest("Investment Policy", "Policy content");

        assertDoesNotThrow(() -> validator.validateCreateDocument(request));
    }

    @Test
    void testRejectsDocumentRequestWithMissingRequiredFields() {
        assertBadRequest(() -> validator.validateCreateDocument(new CreateDocumentRequest(null, "content")), "title is required");
        assertBadRequest(() -> validator.validateCreateDocument(new CreateDocumentRequest("title", " ")), "content is required");
    }

    @Test
    void testRejectsDocumentRequestWithTooLongFields() {
        assertBadRequest(() -> validator.validateCreateDocument(new CreateDocumentRequest(
                "x".repeat(256),
                "content"
        )), "title must be at most 255 characters");
        assertBadRequest(() -> validator.validateCreateDocument(new CreateDocumentRequest(
                "title",
                "x".repeat(1_000_001)
        )), "content must be at most 1000000 characters");
    }

    private static void assertBadRequest(Runnable request, String expectedMessage) {
        var exception = assertThrows(BadRequestException.class, request::run);
        assertThat(exception).hasMessage(expectedMessage);
    }

}
