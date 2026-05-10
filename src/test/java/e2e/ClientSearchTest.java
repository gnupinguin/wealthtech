package e2e;

import io.gnupinguin.nevis.wealthtech.rest.dto.ClientResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import io.gnupinguin.nevis.wealthtech.rest.dto.SearchResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.SocialLinkRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientSearchTest extends AbstractE2eTest {

    @Test
    void testClientSearchByFirstNamePrefixReturnsMatchingClient() {
        createClient("Jonathan", "Doe", "jonathan.doe@example.com", null, List.of());

        var response = search("jonath");

        assertThat(response.clients()).singleElement()
                .satisfies(c -> assertEquals("Jonathan", c.firstName()));
        assertThat(response.errors()).doesNotContain("Client results are unavailable");
    }

    @Test
    void testClientSearchByLastNamePrefixReturnsMatchingClient() {
        createClient("Jane", "Thornton", "jane.thornton@example.com", null, List.of());

        var response = search("thornt");

        assertThat(response.clients()).singleElement()
                .satisfies(c -> assertEquals("Thornton", c.lastName()));
    }

    @Test
    void testClientSearchByEmailPrefixReturnsMatchingClient() {
        createClient("Ada", "Lovelace", "ada.lovelace@example.com", null, List.of());

        var response = search("ada.lovelace");

        assertThat(response.clients()).singleElement()
                .satisfies(c -> assertEquals("ada.lovelace@example.com", c.email()));
    }

    @Test
    void testClientSearchByDescriptionReturnsMatchingClient() {
        createClient("Ada", "Lovelace", "ada@example.com", "technology investor", List.of());

        var response = search("technology");

        assertThat(response.clients()).singleElement()
                .satisfies(c -> assertEquals("Ada", c.firstName()));
    }

    @Test
    void testClientSearchIsCaseInsensitive() {
        createClient("Alice", "Brown", "alice@example.com", null, List.of());

        var response = search("ALICE");

        assertThat(response.clients()).singleElement()
                .satisfies(c -> assertEquals("Alice", c.firstName()));
    }

    @Test
    void testClientSearchReturnsEmptyWhenNoMatch() {
        createClient("John", "Smith", "john.smith@example.com", null, List.of());

        var response = search("zxqwerty");

        assertThat(response.clients()).isEmpty();
    }

    @Test
    void testClientSearchRanksEmailMatchAboveNameMatch() {
        createClient("Growth", "User", "user@example.com", null, List.of());
        createClient("Other", "User", "growth@example.com", null, List.of());

        var response = search("growth");

        assertThat(response.clients()).hasSizeGreaterThanOrEqualTo(2);
        assertEquals("growth@example.com", response.clients().getFirst().email());
    }

    @Test
    void testClientSearchRespectsClientLimit() {
        createClient("Alan", "One", "alan.one@example.com", null, List.of());
        createClient("Alan", "Two", "alan.two@example.com", null, List.of());
        createClient("Alan", "Three", "alan.three@example.com", null, List.of());

        var response = searchWithLimit("alan", 2);

        assertThat(response.clients()).hasSize(2);
    }

    @Test
    void testClientSearchReturnsHydratedClientDataIncludingSocialLinks() {
        createClient("Ada", "Lovelace", "ada@example.com", "Investor",
                List.of(new SocialLinkRequest("https://linkedin.com/in/ada")));

        var response = search("ada");

        assertThat(response.clients()).singleElement()
                .satisfies(c -> {
                    assertThat(c.id()).isNotNull();
                    assertEquals("Ada", c.firstName());
                    assertEquals("Lovelace", c.lastName());
                    assertEquals("ada@example.com", c.email());
                    assertEquals("Investor", c.description());
                    assertThat(c.socialLinks()).containsExactly("https://linkedin.com/in/ada");
                    assertThat(c.score()).isGreaterThan(0f);
                });
    }

    private SearchResponse search(String query) {
        return restTemplate.getForObject("/search?q={q}", SearchResponse.class, query);
    }

    private SearchResponse searchWithLimit(String query, int clientLimit) {
        return restTemplate.getForObject(
                "/search?q={q}&clientLimit={limit}", SearchResponse.class, query, clientLimit);
    }

    private ClientResponse createClient(String firstName, String lastName, String email,
                                        String description, List<SocialLinkRequest> socialLinks) {
        var request = new CreateClientRequest(firstName, lastName, email, description, socialLinks);
        return restTemplate.postForObject("/clients", request, ClientResponse.class);
    }

}
