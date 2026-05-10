package e2e;

import io.gnupinguin.nevis.wealthtech.WealthTechApplication;
import io.gnupinguin.nevis.wealthtech.rest.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Tag("e2e")
@Testcontainers
@EmbeddedKafka(
        partitions = 8,
        topics = {"document-enrichment-events", "document-enrichment-events-dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = WealthTechApplication.class
)
@ActiveProfiles("e2e")
class MultibatchDocumentResponseTest {

    private static final String FIRST_BATCH_MARKER = "amber reserve ladder";
    private static final String SECOND_BATCH_MARKER = "charitable remainder harvest";
    private static final String MATCHED_BATCH_MARKER = "orion hydrogen runway";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbcClient;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));
        restTemplate.setErrorHandler(_ -> false);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM documents").update();
        jdbcClient.sql("DELETE FROM client_social_links").update();
        jdbcClient.sql("DELETE FROM clients").update();
    }

    @Test
    void testLongDocumentSearchResponseContainsOnlyMatchedChunk() throws InterruptedException {
        var client = createClient();
        var content = multibatchDocumentContent();
        var document = createDocument(client.id(), "Large Portfolio Notes", content);

        log.info("Waiting for document chunks to be processed");
        TimeUnit.SECONDS.sleep(7);

        var response = restTemplate.getForEntity(
                "/search?q={query}&documentLimit=1", SearchResponse.class, MATCHED_BATCH_MARKER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.errors()).isEmpty();
        assertThat(body.documents()).singleElement()
                .satisfies(result -> {
                    assertThat(result.id()).isEqualTo(document.id());
                    assertThat(result.clientId()).isEqualTo(client.id());
                    assertThat(result.matchedChunk()).contains(MATCHED_BATCH_MARKER);
                    assertThat(result.matchedChunk()).doesNotContain(FIRST_BATCH_MARKER, SECOND_BATCH_MARKER);
                    assertThat(result.matchedChunk().length()).isLessThan(content.length() / 2);
                });
    }
    private ClientResponse createClient() {
        var request = new CreateClientRequest("Morgan", "Stone", "morgan.stone@example.com", null, null);
        return restTemplate.postForObject("/clients", request, ClientResponse.class);
    }

    private DocumentResponse createDocument(UUID clientId, String title, String content) {
        var request = new CreateDocumentRequest(title, content);
        return restTemplate.postForObject("/clients/{id}/documents", request, DocumentResponse.class, clientId);
    }

    private static String multibatchDocumentContent() {
        return """
                %s

                %s

                %s
                """.formatted(
                repeatedSentence("The " + FIRST_BATCH_MARKER + " section keeps reserves in treasury bills and municipal bonds.", 180),
                repeatedSentence("The " + SECOND_BATCH_MARKER + " section covers trust planning and tax loss harvesting.", 180),
                repeatedSentence("The " + MATCHED_BATCH_MARKER + " section allocates capital to transition infrastructure.", 80)
        );
    }

    private static String repeatedSentence(String sentence, int times) {
        return (sentence + " ").repeat(times).trim();
    }

}
