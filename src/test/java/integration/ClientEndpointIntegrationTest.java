package integration;

import io.gnupinguin.nevis.wealthtech.model.Client;
import io.gnupinguin.nevis.wealthtech.model.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.model.CreateDocumentRequest;
import io.gnupinguin.nevis.wealthtech.model.Document;
import io.gnupinguin.nevis.wealthtech.model.SocialLinkRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientEndpointIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createClient_withAllFields_returns201AndClient() {
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

        ResponseEntity<Client> response = restTemplate.postForEntity("/clients", request, Client.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Client client = response.getBody();
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
    void createClient_withMinimalFields_returns201AndClientWithEmptySocialLinks() {
        var request = new CreateClientRequest(
                "John",
                "Smith",
                "john.smith@example.com",
                null,
                null
        );

        ResponseEntity<Client> response = restTemplate.postForEntity("/clients", request, Client.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Client client = response.getBody();
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
    void createClient_withEmptySocialLinks_returns201AndClientWithEmptySocialLinks() {
        var request = new CreateClientRequest(
                "Alice",
                "Brown",
                "alice.brown@example.com",
                "VIP client",
                List.of()
        );

        ResponseEntity<Client> response = restTemplate.postForEntity("/clients", request, Client.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Client client = response.getBody();
        assertThat(client).isNotNull();
        assertThat(client.socialLinks()).isEmpty();
    }

    @Test
    void createDocument_forExistingClient_returns201AndDocument() {
        Client client = createClient("Bob", "Jones", "bob.jones@example.com");
        var request = new CreateDocumentRequest("Investment Policy", "This is the investment policy content.");

        ResponseEntity<Document> response = restTemplate.postForEntity(
                "/clients/{id}/documents", request, Document.class, client.id()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Document document = response.getBody();
        assertThat(document).isNotNull();
        assertThat(document.id()).isNotNull();
        assertThat(document.clientId()).isEqualTo(client.id());
        assertThat(document.title()).isEqualTo("Investment Policy");
        assertThat(document.content()).isEqualTo("This is the investment policy content.");
        assertThat(document.summary()).isNull();
        assertThat(document.createdAt()).isNotNull();
    }

    @Test
    void createDocument_forNonExistingClient_returns404() {
        var request = new CreateDocumentRequest("Orphan Document", "No client for this.");
        UUID nonExistingClientId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/clients/{id}/documents", request, Void.class, nonExistingClientId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Client createClient(String firstName, String lastName, String email) {
        var request = new CreateClientRequest(firstName, lastName, email, null, null);
        return restTemplate.postForObject("/clients", request, Client.class);
    }
}
