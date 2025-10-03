package br.corp.shortener.services;

import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import br.corp.shortener.exceptions.DuplicateCodeException;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.repositories.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortUrlAccessRepository shortUrlAccessRepository;

    @Mock
    private TopRankingCache topRankingCache;

    @InjectMocks
    private UrlShortenerService service;

    @Captor
    private ArgumentCaptor<ShortUrlAccess> accessCaptor;

    private static ShortUrl buildShortUrl(String url, String code) {
        ShortUrl su = new ShortUrl(url, code, Instant.now());
        su.setId(1L);
        return su;
    }

    @BeforeEach
    void setup() {
        // Por padrão, não bloquear geração nem leitura do cache
        when(topRankingCache.containsCode(anyString())).thenReturn(false);
        when(topRankingCache.getEntity(anyString())).thenReturn(null);
        when(topRankingCache.getTop()).thenReturn(List.of());
    }

    @Test
    @DisplayName("shorten com código customizado livre deve salvar e retornar")
    void shortenCustomCode_ok() {
        when(shortUrlRepository.existsByCode("ABCDE")).thenReturn(false);
        ShortUrl saved = buildShortUrl("https://example.com", "ABCDE");
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenReturn(saved);

        ShortUrl result = service.shorten("https://example.com", "ABCDE");

        assertNotNull(result);
        assertEquals("ABCDE", result.getCode());
        assertEquals("https://example.com", result.getOriginalUrl());
        verify(shortUrlRepository, times(1)).existsByCode("ABCDE");
        verify(shortUrlRepository, times(1)).saveAndFlush(any(ShortUrl.class));
        verifyNoMoreInteractions(shortUrlRepository);
        verifyNoInteractions(shortUrlAccessRepository);
    }

    @Test
    @DisplayName("shorten com código customizado já existente deve lançar DuplicateCodeException")
    void shortenCustomCode_duplicate() {
        when(shortUrlRepository.existsByCode("ABCDE")).thenReturn(true);

        DuplicateCodeException ex = assertThrows(DuplicateCodeException.class,
                () -> service.shorten("https://example.com", "ABCDE"));

        assertTrue(ex.getMessage().contains("ABCDE"));
        verify(shortUrlRepository, times(1)).existsByCode("ABCDE");
        verifyNoMoreInteractions(shortUrlRepository);
        verifyNoInteractions(shortUrlAccessRepository);
    }

    @Test
    @DisplayName("shorten com código customizado: corrida/violação de unicidade mapeia para DuplicateCodeException")
    void shortenCustomCode_uniqueViolationMapsToDuplicate() {
        when(shortUrlRepository.existsByCode("ABCD1")).thenReturn(false);
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("unique_violation"));

        assertThrows(DuplicateCodeException.class,
                () -> service.shorten("https://ex.com", "ABCD1"));

        verify(shortUrlRepository).existsByCode("ABCD1");
        verify(shortUrlRepository).saveAndFlush(any(ShortUrl.class));
        verifyNoMoreInteractions(shortUrlRepository);
    }

    @Test
    @DisplayName("shorten sem código: salva com código aleatório na primeira tentativa")
    void shortenRandom_firstTry() {
        ShortUrl saved = buildShortUrl("https://example.com", "Zy12A");
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenReturn(saved);

        ShortUrl result = service.shorten("https://example.com", null);

        assertNotNull(result);
        assertEquals("Zy12A", result.getCode());
        assertEquals("https://example.com", result.getOriginalUrl());
        verify(shortUrlRepository, times(1)).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    @DisplayName("shorten sem código: colide 2x e depois salva (retentativa)")
    void shortenRandom_retriesOnCollision() {
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("collision1"))
                .thenThrow(new DataIntegrityViolationException("collision2"))
                .thenReturn(buildShortUrl("https://ex.com", "A1b2C"));

        ShortUrl result = service.shorten("https://ex.com", null);

        assertNotNull(result);
        assertEquals("A1b2C", result.getCode());
        verify(shortUrlRepository, times(3)).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    @DisplayName("shorten sem código: excede tentativas e lança IllegalStateException")
    void shortenRandom_exceedsMaxAttempts() {
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("c1"))
                .thenThrow(new DataIntegrityViolationException("c2"))
                .thenThrow(new DataIntegrityViolationException("c3"))
                .thenThrow(new DataIntegrityViolationException("c4"))
                .thenThrow(new DataIntegrityViolationException("c5"));

        assertThrows(IllegalStateException.class, () -> service.shorten("https://ex.com", null));
        verify(shortUrlRepository, times(5)).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    @DisplayName("getByCode encontra e retorna ShortUrl")
    void getByCode_found() {
        ShortUrl su = buildShortUrl("https://ex.com", "Abc12");
        when(shortUrlRepository.findByCode("Abc12")).thenReturn(Optional.of(su));

        ShortUrl result = service.getByCode("Abc12");
        assertNotNull(result);
        assertEquals("Abc12", result.getCode());
    }

    @Test
    @DisplayName("getByCode não encontra e retorna null")
    void getByCode_notFound() {
        when(shortUrlRepository.findByCode("XyZ99")).thenReturn(Optional.empty());
        assertNull(service.getByCode("XyZ99"));
    }

    @Test
    @DisplayName("registerAccess persiste acesso com os campos esperados")
    void registerAccess_persistsAccess() {
        ShortUrl su = buildShortUrl("https://ex.com", "QwErT");
        when(shortUrlAccessRepository.save(any(ShortUrlAccess.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.registerAccess(su, "UA-1", "ref-1");

        verify(shortUrlAccessRepository).save(accessCaptor.capture());
        ShortUrlAccess captured = accessCaptor.getValue();
        assertNotNull(captured);
        assertEquals(su, captured.getShortUrl());
        assertEquals("UA-1", captured.getUserAgent());
        assertEquals("ref-1", captured.getReferer());
        assertNotNull(captured.getAccessedAt());
    }

    @Test
    @DisplayName("ranking delega para o cache e retorna lista")
    void ranking_returnsCacheData() {
        List<RankingItem> items = List.of(
                new RankingItem("ABC12", "https://ex.com/a", 3L),
                new RankingItem("XYz90", "https://ex.com/b", 1L)
        );
        when(topRankingCache.getTop()).thenReturn(items);

        List<RankingItem> result = service.ranking();
        assertEquals(items, result);
        verify(topRankingCache, times(1)).getTop();
        verifyNoInteractions(shortUrlRepository);
    }
}