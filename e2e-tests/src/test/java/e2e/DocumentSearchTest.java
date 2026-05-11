package e2e;

import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class DocumentSearchTest extends AbstractApiTest {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchTest.class);

    @Test
    public void createdDocumentBecomesSearchableThroughAsyncEnrichment() {
        var token = uniqueToken("doc");
        log.info("Starting document search scenario token={}", token);
        log.info("Step: create client fixture for document search");
        var clientId = createClient(token);
        log.info("Step: create document fixture for async enrichment");
        var document = api.createDocument(
                clientId,
                "Transition Infrastructure " + token,
                """
                The %s allocation plan focuses on hydrogen electrolysers, grid-scale battery storage,
                transmission upgrades, and low-carbon logistics infrastructure. The client wants a
                long-term transition portfolio with staged capital calls and clear liquidity reserves.
                """.formatted(token)
        );
        var documentId = document.jsonPath().getString("id");

        log.info("Step: wait until document id={} is searchable", documentId);
        awaitDocumentSearchResult(
                "hydrogen transition infrastructure " + token,
                documentId,
                clientId,
                token,
                "hydrogen"
        );
        log.info("Finished document search scenario token={}", token);
    }

    @Test
    public void longDocumentSearchReturnsOnlyTheMatchedChunk() {
        var token = uniqueToken("longdoc");
        log.info("Starting long-document matched-chunk scenario token={}", token);
        log.info("Step: create client fixture for long-document search");
        var clientId = createClient(token);
        var firstMarker = "amber reserve ladder " + token;
        var secondMarker = "charitable remainder harvest " + token;
        var matchedMarker = "orion hydrogen runway " + token;
        log.info("Step: build long document content with matched marker '{}'", matchedMarker);
        var content = """
                %s

                %s

                %s
                """.formatted(
                repeatedSentence("The " + firstMarker + " section keeps reserves in treasury bills and municipal bonds.", 180),
                repeatedSentence("The " + secondMarker + " section covers trust planning and tax loss harvesting.", 180),
                repeatedSentence("The " + matchedMarker + " section allocates capital to transition infrastructure.", 80)
        );
        log.info("Step: create long document fixture");
        var document = api.createDocument(clientId, "Large Portfolio Notes " + token, content);
        var documentId = document.jsonPath().getString("id");

        log.info("Step: wait until long document id={} returns only matched chunk", documentId);
        await("document chunk for " + documentId)
                .ignoreExceptions()
                .pollDelay(Duration.ofSeconds(5))
                .pollInterval(Duration.ofSeconds(5))
                .atMost(Duration.ofSeconds(150))
                .untilAsserted(() -> {
                    log.info("Polling search for matched marker '{}'", matchedMarker);
                    var search = api.search(matchedMarker, 1, 5);
                    assertThat(search.jsonPath().getList("errors", String.class)).isEmpty();

                    var result = documentById(search, documentId);
                    assertThat(result).isNotNull();
                    assertThat(result.get("client_id")).isEqualTo(clientId);

                    var matchedChunk = (String) result.get("matched_chunk");
                    assertThat(matchedChunk).contains(matchedMarker);
                    assertThat(matchedChunk).doesNotContain(firstMarker, secondMarker);
                    assertThat(matchedChunk.length()).isLessThan(content.length() / 2);
                });
        log.info("Finished long-document matched-chunk scenario token={}", token);
    }

    private void awaitDocumentSearchResult(String query,
                                           String documentId,
                                           String clientId,
                                           String... expectedMatchedChunkTerms) {
        log.info(
                "Awaiting document search result documentId={}, clientId={}, query='{}'",
                documentId,
                clientId,
                query
        );
        await("document search result for " + documentId)
                .ignoreExceptions()
                .pollDelay(Duration.ofSeconds(5))
                .pollInterval(Duration.ofSeconds(5))
                .atMost(Duration.ofSeconds(120))
                .untilAsserted(() -> {
                    log.info("Polling document search result documentId={}", documentId);
                    var search = api.search(query, 1, 5);
                    assertThat(search.jsonPath().getList("errors", String.class)).isEmpty();

                    var result = documentById(search, documentId);
                    assertThat(result).isNotNull();
                    assertThat(result.get("client_id")).isEqualTo(clientId);
                    assertThat((String) result.get("matched_chunk")).contains(expectedMatchedChunkTerms);
                });
        log.info("Document search result is available documentId={}", documentId);
    }

    private String createClient(String token) {
        log.info("Creating document-search client fixture token={}", token);
        return api.createClient(
                        "Morgan",
                        "Stone" + token,
                        token + "@example.test",
                        "Document search fixture " + token,
                        null
                )
                .jsonPath()
                .getString("id");
    }

    private static Map<?, ?> documentById(Response search, String documentId) {
        return search.jsonPath().getList("documents", Map.class).stream()
                .filter(document -> documentId.equals(document.get("id")))
                .findFirst()
                .orElse(null);
    }

    private static String repeatedSentence(String sentence, int times) {
        return (sentence + " ").repeat(times).trim();
    }
}
