package integration;

import io.gnupinguin.nevis.wealthtech.rest.dto.ClientResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateDocumentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientEndpointValidationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void testCreateClientWithMissingFirstNameReturns400() {
        var request = new CreateClientRequest(
                null,
                "Doe",
                "jane.doe@example.com",
                "Wealth management client",
                null
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("first_name is required", response.getBody());
    }

    @Test
    void testCreateClientWithBlankFirstNameReturns400() {
        var request = new CreateClientRequest(
                "   ",
                "Doe",
                "jane.doe@example.com",
                null,
                null
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("first_name is required", response.getBody());
    }

    @Test
    void testCreateClientWithMissingLastNameReturns400() {
        var request = new CreateClientRequest(
                "Jane",
                null,
                "jane.doe@example.com",
                null,
                null
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("last_name is required", response.getBody());
    }

    @Test
    void testCreateClientWithBlankLastNameReturns400() {
        var request = new CreateClientRequest(
                "Jane",
                "   ",
                "jane.doe@example.com",
                null,
                null
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("last_name is required", response.getBody());
    }

    @Test
    void testCreateClientWithMissingEmailReturns400() {
        var request = new CreateClientRequest(
                "Jane",
                "Doe",
                null,
                null,
                null
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("email is required", response.getBody());
    }

    @Test
    void testCreateClientWithInvalidEmailReturns400() {
        var request = new CreateClientRequest(
                "Jane",
                "Doe",
                "not-email",
                "Wealth management client",
                null
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("email must be a valid email address", response.getBody());
    }

    @Test
    void testCreateClientWithTooLongDescriptionReturns400() {
        var request = new CreateClientRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "x".repeat(4097),
                null
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("description must be at most 4096 characters", response.getBody());
    }

    @Test
    void testCreateClientWithBlankSocialLinkReturns400() {
        var request = new CreateClientRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "Wealth management client",
                List.of(" ")
        );

        var response = restTemplate.postForEntity("/clients", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("social_links entries must be non-blank", response.getBody());
    }

    @Test
    void testCreateDocumentWithMissingTitleReturns400() {
        var client = createClient();
        var request = new CreateDocumentRequest(" ", "This is the investment policy content.");

        var response = restTemplate.postForEntity(
                "/clients/{id}/documents", request, String.class, client.id()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("title is required", response.getBody());
    }

    @Test
    void testCreateDocumentWithMissingContentReturns400() {
        var client = createClient();
        var request = new CreateDocumentRequest("Investment Policy", null);

        var response = restTemplate.postForEntity(
                "/clients/{id}/documents", request, String.class, client.id()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("content is required", response.getBody());
    }

    @Test
    void testCreateDocumentWithBlankContentReturns400() {
        var client = createClient();
        var request = new CreateDocumentRequest("Investment Policy", "   ");

        var response = restTemplate.postForEntity(
                "/clients/{id}/documents", request, String.class, client.id()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("content is required", response.getBody());
    }

    private ClientResponse createClient() {
        var request = new CreateClientRequest("Bob", "Jones", "bob.jones@example.com", null, null);
        return restTemplate.postForObject("/clients", request, ClientResponse.class);
    }

}
