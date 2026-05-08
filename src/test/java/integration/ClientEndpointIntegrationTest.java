package integration;

import io.gnupinguin.nevis.wealthtech.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientEndpointIntegrationTest extends AbstractIntegrationTest {

    @Test
    void testCreateClientWithAllFieldsReturns201AndClient() {
        var request = new CreateClientRequest(
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "Wealth management client",
                List.of(
                        new SocialLinkRequest("https://linkedin.com/in/janedoe"),
                        new SocialLinkRequest("https://twitter.com/janedoe")
                )
        );

        var response = restTemplate.postForEntity("/clients", request, Client.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var client = response.getBody();
        assertThat(client).isNotNull();
        assertThat(client.id()).isNotNull();
        assertEquals("Jane", client.firstName());
        assertEquals("Doe", client.lastName());
        assertEquals("jane.doe@example.com", client.email());
        assertEquals("Wealth management client", client.description());
        assertThat(client.createdAt()).isNotNull();
        assertThat(client.socialLinks()).hasSize(2);
        assertThat(client.socialLinks()).anySatisfy(s -> {
            assertEquals("https://linkedin.com/in/janedoe", s.url());
            assertThat(s.id()).isNotNull();
            assertThat(s.createdAt()).isNotNull();
        });
        assertThat(client.socialLinks()).anySatisfy(s ->
            assertEquals("https://twitter.com/janedoe", s.url())
        );
    }

    @Test
    void testCreateClientWithMinimalFieldsReturns201AndClientWithEmptySocialLinks() {
        var request = new CreateClientRequest(
                "John",
                "Smith",
                "john.smith@example.com",
                null,
                null
        );

        var response = restTemplate.postForEntity("/clients", request, Client.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var client = response.getBody();
        assertThat(client).isNotNull();
        assertThat(client.id()).isNotNull();
        assertEquals("John", client.firstName());
        assertEquals("Smith", client.lastName());
        assertEquals("john.smith@example.com", client.email());
        assertThat(client.description()).isNull();
        assertThat(client.createdAt()).isNotNull();
        assertThat(client.socialLinks()).isEmpty();
    }

    @Test
    void testCreateClientWithEmptySocialLinksReturns201AndClientWithEmptySocialLinks() {
        var request = new CreateClientRequest(
                "Alice",
                "Brown",
                "alice.brown@example.com",
                "VIP client",
                List.of()
        );

        var response = restTemplate.postForEntity("/clients", request, Client.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var client = response.getBody();
        assertThat(client).isNotNull();
        assertThat(client.socialLinks()).isEmpty();
    }

    @Test
    void testCreateDocumentForExistingClientReturns201AndDocument() {
        var client = createClient("Bob", "Jones", "bob.jones@example.com");
        var request = new CreateDocumentRequest("Investment Policy", "This is the investment policy content.");

        var response = restTemplate.postForEntity(
                "/clients/{id}/documents", request, Document.class, client.id()
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var document = response.getBody();
        assertThat(document).isNotNull();
        assertThat(document.id()).isNotNull();
        assertEquals(client.id(), document.clientId());
        assertEquals("Investment Policy", document.title());
        assertEquals("This is the investment policy content.", document.content());
        assertThat(document.summary()).isNull();
        assertThat(document.createdAt()).isNotNull();
    }

    @Test
    void testCreateDocumentForNonExistingClientReturns404() {
        var request = new CreateDocumentRequest("Orphan Document", "No client for this.");
        var nonExistingClientId = UUID.randomUUID();

        var response = restTemplate.postForEntity(
                "/clients/{id}/documents", request, Void.class, nonExistingClientId
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Client createClient(String firstName, String lastName, String email) {
        var request = new CreateClientRequest(firstName, lastName, email, null, null);
        return restTemplate.postForObject("/clients", request, Client.class);
    }
}
