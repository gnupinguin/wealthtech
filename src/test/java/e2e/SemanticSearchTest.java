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
import static org.junit.jupiter.api.Assertions.assertEquals;

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
class SemanticSearchTest {

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
        restTemplate.setErrorHandler((_) -> false);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM documents").update();
        jdbcClient.sql("DELETE FROM client_social_links").update();
        jdbcClient.sql("DELETE FROM clients").update();
    }

    @Test
    void testSearchWithNoDocumentsReturnsEmptyResult() {
        var response = restTemplate.getForObject("/search?q=investment strategy", SearchResponse.class);

        assertThat(response).isNotNull();
        assertEquals("investment strategy", response.query());
        assertThat(response.documents()).isEmpty();
        assertThat(response.clients()).isEmpty();
    }

    @Test
    void testSearchRanksSemanticallyRelevantDocumentHigher() throws InterruptedException {
        var techClient = createClient("John", "Tech", "john.tech@example.com");
        var estateClient = createClient("Jane", "Estate", "jane.estate@example.com");

        var techDoc = createDocument(techClient.id(), "Technology Portfolio",
                """
                This client focuses on technology sector investments including AI companies,
                cloud computing, semiconductor manufacturers, and software development firms.
                Holdings include major tech stocks such as Apple, Google, Microsoft, and NVIDIA.
                """);

        var estateDoc = createDocument(estateClient.id(), "Real Estate Portfolio",
                """
                This client invests in commercial real estate, residential rental properties,
                and REITs. The portfolio includes office buildings, apartment complexes, and
                shopping centers with focus on stable rental income and property appreciation.
                """);

        log.info("Test pause for async synchronization");
        TimeUnit.SECONDS.sleep(5);
        var techSearch = restTemplate.getForObject(
                "/search?q=AI semiconductor technology stocks", SearchResponse.class);

        assertThat(techSearch).isNotNull();
        assertThat(techSearch.documents()).isNotEmpty();
        assertEquals(techDoc.id(), techSearch.documents().getFirst().id());

        var estateSearch = restTemplate.getForObject(
                "/search?q=rental property real estate REIT", SearchResponse.class);

        assertThat(estateSearch).isNotNull();
        assertThat(estateSearch.documents()).isNotEmpty();
        assertEquals(estateDoc.id(), estateSearch.documents().getFirst().id());
    }

    private ClientResponse createClient(String firstName, String lastName, String email) {
        var request = new CreateClientRequest(firstName, lastName, email, null, null);
        return restTemplate.postForObject("/clients", request, ClientResponse.class);
    }

    private DocumentResponse createDocument(UUID clientId, String title, String content) {
        var request = new CreateDocumentRequest(title, content);
        return restTemplate.postForObject("/clients/{id}/documents", request, DocumentResponse.class, clientId);
    }
}
