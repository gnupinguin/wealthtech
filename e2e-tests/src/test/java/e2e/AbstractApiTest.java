package e2e;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

abstract class AbstractApiTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractApiTest.class);

    protected final WealthTechApi api = new WealthTechApi();

    @BeforeSuite(alwaysRun = true)
    public void configureHttpClient() {
        var baseUrl = configuredBaseUrl();
        var managementBaseUrl = configuredManagementBaseUrl(baseUrl);
        log.info("Configuring REST Assured base URI: {}", baseUrl);
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.filters(
                new RequestLoggingFilter(LogDetail.ALL),
                new ResponseLoggingFilter(LogDetail.ALL)
        );
        RestAssured.config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 5_000)
                        .setParam("http.socket.timeout", 15_000));

        log.info("Waiting for WealthTech application health endpoint to report UP: {}", managementBaseUrl);
        await("WealthTech application is healthy")
                .ignoreExceptions()
                .pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> given()
                        .baseUri(managementBaseUrl)
                        .when()
                        .get("/actuator/health")
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("UP")));
        log.info("WealthTech application is healthy");
    }

    protected static String uniqueToken(String prefix) {
        var suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return (prefix + suffix).toLowerCase(Locale.ROOT);
    }

    private static String configuredBaseUrl() {
        var baseUrl = System.getProperty("e2e.baseUrl");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8080");
        }
        return baseUrl;
    }

    private static String configuredManagementBaseUrl(String apiBaseUrl) {
        var managementBaseUrl = System.getProperty("e2e.managementBaseUrl");
        if (managementBaseUrl == null || managementBaseUrl.isBlank()) {
            managementBaseUrl = System.getenv("E2E_MANAGEMENT_BASE_URL");
        }
        if (managementBaseUrl == null || managementBaseUrl.isBlank()) {
            managementBaseUrl = defaultManagementBaseUrl(apiBaseUrl);
        }
        return managementBaseUrl;
    }

    private static String defaultManagementBaseUrl(String apiBaseUrl) {
        try {
            var uri = URI.create(apiBaseUrl);
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), 8081, null, null, null).toString();
        } catch (IllegalArgumentException | URISyntaxException e) {
            log.warn("Could not derive management URL from API base URL '{}', falling back to localhost:8081", apiBaseUrl, e);
            return "http://localhost:8081";
        }
    }
}
