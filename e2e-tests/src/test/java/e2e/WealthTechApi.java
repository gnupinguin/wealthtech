package e2e;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

final class WealthTechApi {

    private static final Logger log = LoggerFactory.getLogger(WealthTechApi.class);

    Response createClient(String firstName,
                          String lastName,
                          String email,
                          String description,
                          List<String> socialLinks) {
        log.info("Creating client firstName={}, lastName={}, email={}", firstName, lastName, email);
        var body = new LinkedHashMap<String, Object>();
        body.put("first_name", firstName);
        body.put("last_name", lastName);
        body.put("email", email);
        body.put("description", description);
        body.put("social_links", socialLinks == null ? null : socialLinks.stream()
                .map(url -> Map.of("url", url))
                .toList());

        var response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/clients")
                .then()
                .statusCode(HttpURLConnection.HTTP_CREATED)
                .extract()
                .response();
        log.info("Created client id={}", response.jsonPath().getString("id"));
        return response;
    }

    Response createDocument(String clientId, String title, String content) {
        log.info("Creating document for clientId={}, title={}", clientId, title);
        var body = new LinkedHashMap<String, Object>();
        body.put("title", title);
        body.put("content", content);

        var response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .pathParam("clientId", clientId)
                .when()
                .post("/clients/{clientId}/documents")
                .then()
                .statusCode(HttpURLConnection.HTTP_CREATED)
                .extract()
                .response();
        log.info("Created document id={} for clientId={}", response.jsonPath().getString("id"), clientId);
        return response;
    }

    Response search(String query, int clientLimit, int documentLimit) {
        log.info("Searching query='{}', clientLimit={}, documentLimit={}", query, clientLimit, documentLimit);
        var response = given()
                .queryParam("q", query)
                .queryParam("clientLimit", clientLimit)
                .queryParam("documentLimit", documentLimit)
                .when()
                .get("/search")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract()
                .response();
        log.info(
                "Search completed query='{}', clients={}, documents={}, errors={}",
                query,
                response.jsonPath().getList("clients").size(),
                response.jsonPath().getList("documents").size(),
                response.jsonPath().getList("errors").size()
        );
        return response;
    }
}
