package br.corp.shortener.controllers;

import br.corp.shortener.dto.ErrorResponse;
import br.corp.shortener.dto.StatsResponse;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.services.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock
    private UrlShortenerService service;

    @Mock
    private ShortUrlAccessRepository accessRepository;

    @InjectMocks
    private StatsController controller;

    private static ShortUrl su(String url, String code) {
        return ShortUrl.builder().id(7L).originalUrl(url).code(code).createdAt(Instant.now()).build();
    }

    @Test
    @DisplayName("stats 200 retorna body com hits do repositório")
    void stats_ok() {
        ShortUrl shortUrl = su("https://ex.com", "ABCDE");
        when(service.getByCode("ABCDE")).thenReturn(shortUrl);
        when(accessRepository.countByShortUrl(shortUrl)).thenReturn(10L);

        ResponseEntity<?> resp = controller.stats("ABCDE");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody() instanceof StatsResponse);
        StatsResponse body = (StatsResponse) resp.getBody();
        assertEquals("ABCDE", body.code());
        assertEquals("https://ex.com", body.originalUrl());
        assertEquals(10L, body.hits());
    }

    @Test
    @DisplayName("stats 404 quando code não existe")
    void stats_notFound() {
        when(service.getByCode("XXXXX")).thenReturn(null);
        ResponseEntity<?> resp = controller.stats("XXXXX");
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertTrue(resp.getBody() instanceof ErrorResponse);
    }
}