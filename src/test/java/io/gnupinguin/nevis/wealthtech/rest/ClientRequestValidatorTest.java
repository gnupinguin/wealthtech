package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.exception.BadRequestException;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.SocialLinkRequest;
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
                List.of(new SocialLinkRequest("https://example.com/ada"))
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
    void testRejectsClientRequestWithBlankSocialLink() {
        var request = new CreateClientRequest(
                "Ada",
                "Lovelace",
                "ada@example.com",
                null,
                List.of(new SocialLinkRequest(" "))
        );

        assertBadRequest(() -> validator.validateCreateClient(request), "social_links.url is required");
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

    private static void assertBadRequest(Runnable request, String expectedMessage) {
        var exception = assertThrows(BadRequestException.class, request::run);
        assertThat(exception).hasMessage(expectedMessage);
    }

}
