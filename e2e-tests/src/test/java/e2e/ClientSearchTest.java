package e2e;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientSearchTest extends AbstractApiTest {

    private static final Logger log = LoggerFactory.getLogger(ClientSearchTest.class);

    @Test
    public void createsClientAndFindsItByUniqueProfileData() {
        var token = uniqueToken("client");
        var lastName = "Search" + token;
        var email = token + "@example.test";
        var socialLink = "https://linkedin.com/in/" + token;

        log.info("Starting client search scenario token={}", token);
        log.info("Step: create a client with unique searchable profile data");
        var created = api.createClient(
                "Alden",
                lastName,
                email,
                "Private markets mandate " + token,
                List.of(socialLink)
        );

        var clientId = created.jsonPath().getString("id");
        assertThat(clientId).isNotBlank();
        assertThat(created.jsonPath().getString("last_name")).isEqualTo(lastName);
        assertThat(created.jsonPath().getList("social_links.url", String.class)).containsExactly(socialLink);

        log.info("Step: search for the created client by last name {}", lastName);
        var search = api.search(lastName, 5, 1);
        assertThat(search.jsonPath().getList("errors", String.class))
                .doesNotContain("Client results are unavailable");

        log.info("Step: verify search response contains hydrated client id={}", clientId);
        assertThat(search.jsonPath().getList("clients", Map.class))
                .anySatisfy(client -> {
                    assertThat(client.get("id")).isEqualTo(clientId);
                    assertThat(client.get("last_name")).isEqualTo(lastName);
                    assertThat(client.get("email")).isEqualTo(email);
                    assertThat(client.get("social_links")).isEqualTo(List.of(socialLink));
                    assertThat(((Number) client.get("score")).floatValue()).isGreaterThan(0f);
                });
        log.info("Finished client search scenario token={}", token);
    }
}
