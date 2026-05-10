package e2e;

import io.gnupinguin.nevis.wealthtech.WealthTechApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import testsupport.SharedPostgresContainer;

@Tag("e2e")
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
abstract class AbstractE2eTest {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgresContainer::jdbcUrl);
        registry.add("spring.datasource.username", SharedPostgresContainer::username);
        registry.add("spring.datasource.password", SharedPostgresContainer::password);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbcClient;

    protected RestTemplate restTemplate;

    @BeforeEach
    void setUpRestTemplate() {
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
}
