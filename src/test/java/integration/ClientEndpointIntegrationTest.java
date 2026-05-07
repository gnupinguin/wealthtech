package integration;

import io.gnupinguin.nevis.wealthtech.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var client = response.getBody();
        assertThat(client).isNotNull();
        assertThat(client.id()).isNotNull();
        assertThat(client.firstName()).isEqualTo("Jane");
        assertThat(client.lastName()).isEqualTo("Doe");
        assertThat(client.email()).isEqualTo("jane.doe@example.com");
        assertThat(client.description()).isEqualTo("Wealth management client");
        assertThat(client.createdAt()).isNotNull();
        assertThat(client.socialLinks()).hasSize(2);
        assertThat(client.socialLinks()).anySatisfy(s -> {
            assertThat(s.url()).isEqualTo("https://linkedin.com/in/janedoe");
            assertThat(s.id()).isNotNull();
            assertThat(s.createdAt()).isNotNull();
        });
        assertThat(client.socialLinks()).anySatisfy(s ->
            assertThat(s.url()).isEqualTo("https://twitter.com/janedoe")
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var client = response.getBody();
        assertThat(client).isNotNull();
        assertThat(client.id()).isNotNull();
        assertThat(client.firstName()).isEqualTo("John");
        assertThat(client.lastName()).isEqualTo("Smith");
        assertThat(client.email()).isEqualTo("john.smith@example.com");
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var document = response.getBody();
        assertThat(document).isNotNull();
        assertThat(document.id()).isNotNull();
        assertThat(document.clientId()).isEqualTo(client.id());
        assertThat(document.title()).isEqualTo("Investment Policy");
        assertThat(document.content()).isEqualTo("This is the investment policy content.");
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Client createClient(String firstName, String lastName, String email) {
        var request = new CreateClientRequest(firstName, lastName, email, null, null);
        return restTemplate.postForObject("/clients", request, Client.class);
    }
}
