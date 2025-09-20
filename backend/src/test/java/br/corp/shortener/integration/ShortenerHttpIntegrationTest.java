package br.corp.shortener.integration;

import br.corp.shortener.dto.ErrorResponse;
import br.corp.shortener.dto.ShortenRequest;
import br.corp.shortener.dto.ShortenResponse;
import br.corp.shortener.dto.ValidationErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ShortenerHttpIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate http;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        http = new RestTemplate();
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @DisplayName("POST /shorten (201) cria um short link e retorna Location e body")
    void shorten201() {
        ShortenRequest req = new ShortenRequest("https://example.com/", null);
        ResponseEntity<ShortenResponse> resp = http.postForEntity(baseUrl + "/shorten", req, ShortenResponse.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getHeaders().getLocation());
        ShortenResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("https://example.com/", body.originalUrl());
        assertNotNull(body.code());
    }

    @Test
    @DisplayName("POST /shorten (400) quando url vazia ou inválida")
    void shorten400() {
        ShortenRequest req = new ShortenRequest("", null);
        try {
            http.postForEntity(baseUrl + "/shorten", req, ValidationErrorResponse.class);
            fail("Deveria lançar 400");
        } catch (HttpClientErrorException.BadRequest ex) {
            assertEquals(400, ex.getStatusCode().value());
        }
    }

    @Test
    @DisplayName("GET /redirect?code=... (404) quando código não existe")
    void redirect404() {
        try {
            http.getForEntity(baseUrl + "/redirect?code=XXXXX", ErrorResponse.class);
            fail("Deveria lançar 404");
        } catch (HttpClientErrorException.NotFound ex) {
            assertEquals(404, ex.getStatusCode().value());
        }
    }

    @Test
    @DisplayName("POST /shorten (409) quando code customizado já existe")
    void shorten409_conflict_custom_code() {
        // Arrange: cria um link com code fixo
        ShortenRequest first = new ShortenRequest("https://example.com/one", "ABCDE");
        ResponseEntity<ShortenResponse> created = http.postForEntity(baseUrl + "/shorten", first, ShortenResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());

        // Act + Assert: tenta criar novamente com o mesmo code
        ShortenRequest dup = new ShortenRequest("https://example.com/two", "ABCDE");
        try {
            http.postForEntity(baseUrl + "/shorten", dup, ErrorResponse.class);
            fail("Deveria lançar 409 CONFLICT");
        } catch (HttpClientErrorException.Conflict ex) {
            assertEquals(409, ex.getStatusCode().value());
        }
    }

    @Test
    @DisplayName("GET /stats/{code} (200) após criação retorna hits=0 e dados corretos")
    void stats200_after_creation_has_zero_hits() {
        // Arrange: cria o short link
        ShortenRequest req = new ShortenRequest("https://example.org/", "QWERT");
        ResponseEntity<ShortenResponse> created = http.postForEntity(baseUrl + "/shorten", req, ShortenResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());

        // Act: consulta stats
        ResponseEntity<String> resp = http.getForEntity(baseUrl + "/stats/QWERT", String.class);

        // Assert: 200 e contém code e hits=0
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"code\":\"QWERT\""));
        assertTrue(body.contains("\"hits\":0"));
    }
}