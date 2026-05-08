package io.gnupinguin.nevis.wealthtech.rest;

import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.SocialLinkRequest;
import io.gnupinguin.nevis.wealthtech.rest.validation.ClientRequestValidator;
import io.gnupinguin.nevis.wealthtech.rest.validation.DefaultClientRequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        )));
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "Ada", " ", "ada@example.com", null, null
        )));
        assertBadRequest(() -> validator.validateCreateClient(new CreateClientRequest(
                "Ada", "Lovelace", null, null, null
        )));
    }

    @Test
    void testRejectsClientRequestWithInvalidEmail() {
        var request = new CreateClientRequest("Ada", "Lovelace", "not-email", null, null);

        assertBadRequest(() -> validator.validateCreateClient(request));
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

        assertBadRequest(() -> validator.validateCreateClient(request));
    }

    @Test
    void testAcceptsValidDocumentRequest() {
        var request = new CreateDocumentRequest("Investment Policy", "Policy content");

        assertDoesNotThrow(() -> validator.validateCreateDocument(request));
    }

    @Test
    void testRejectsDocumentRequestWithMissingRequiredFields() {
        assertBadRequest(() -> validator.validateCreateDocument(new CreateDocumentRequest(null, "content")));
        assertBadRequest(() -> validator.validateCreateDocument(new CreateDocumentRequest("title", " ")));
    }

    private static void assertBadRequest(Runnable request) {
        var exception = assertThrows(ResponseStatusException.class, request::run);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
