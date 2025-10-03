package br.corp.shortener.controllers;

import br.corp.shortener.dto.ErrorResponse;
import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.dto.ShortenRequest;
import br.corp.shortener.dto.ShortenResponse;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.exceptions.DuplicateCodeException;
import br.corp.shortener.services.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkControllerTest {

    @Mock
    private UrlShortenerService service;

    @InjectMocks
    private LinkController controller;

    private static ShortUrl makeSu(String url, String code) {
        ShortUrl su = new ShortUrl(url, code, Instant.now());
        su.setId(1L);
        return su;
    }

    @Test
    @DisplayName("shorten retorna 201 e body correto")
    void shorten_ok() {
        ShortUrl su = makeSu("https://ex.com", "ABCDE");
        when(service.shorten("https://ex.com", null)).thenReturn(su);

        ShortenRequest req = new ShortenRequest("https://ex.com", null);

        ResponseEntity<ShortenResponse> resp = controller.shorten(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(URI.create("/ABCDE"), resp.getHeaders().getLocation());
        ShortenResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(1L, body.id());
        assertEquals("https://ex.com", body.originalUrl());
        assertEquals("ABCDE", body.code());
    }

    @Test
    @DisplayName("ranking retorna lista do service")
    void ranking_ok() {
        List<RankingItem> items = List.of(new RankingItem("AAA11", "https://ex.com", 2L));
        when(service.ranking()).thenReturn(items);

        List<RankingItem> resp = controller.ranking();
        assertEquals(items, resp);
        verify(service).ranking();
    }

    @Test
    @DisplayName("redirect 302 quando encontra o code e registra acesso")
    void redirect_found() {
        when(service.getByCode("ABCDE")).thenReturn(makeSu("https://ex.com", "ABCDE"));

        ResponseEntity<?> resp = controller.redirect("ABCDE", "UA-1", "ref-1");

        assertEquals(HttpStatus.FOUND, resp.getStatusCode());
        assertEquals("https://ex.com", resp.getHeaders().getFirst("Location"));
        verify(service).registerAccess(any(ShortUrl.class), eq("UA-1"), eq("ref-1"));
    }

    @Test
    @DisplayName("redirect 404 quando não encontra o code")
    void redirect_notFound() {
        when(service.getByCode("XXXXX")).thenReturn(null);

        ResponseEntity<?> resp = controller.redirect("XXXXX", null, null);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        Object body = resp.getBody();
        assertTrue(body instanceof ErrorResponse);
    }

    @Test
    @DisplayName("redirect 500 quando ocorre exceção no service")
    void redirect_error() {
        when(service.getByCode("ERR00")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> resp = controller.redirect("ERR00", null, null);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertTrue(resp.getBody() instanceof ErrorResponse);
    }
}