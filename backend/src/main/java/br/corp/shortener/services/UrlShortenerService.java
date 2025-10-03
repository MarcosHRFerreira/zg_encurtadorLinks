package br.corp.shortener.services;

import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.dto.StatsResponse;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import br.corp.shortener.exceptions.DuplicateCodeException;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.repositories.ShortUrlRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class UrlShortenerService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 5;
    private final Random random = new SecureRandom();

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlAccessRepository shortUrlAccessRepository;
    private final TopRankingCache topRankingCache;

    public UrlShortenerService(ShortUrlRepository shortUrlRepository, ShortUrlAccessRepository shortUrlAccessRepository, TopRankingCache topRankingCache) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlAccessRepository = shortUrlAccessRepository;
        this.topRankingCache = topRankingCache;
    }

    @Transactional
    public ShortUrl shorten(String originalUrl, String customCode) {
        log.info("Shorten requested: originalUrl={}, customCodeProvided={}", originalUrl, customCode != null);
        if (customCode != null) {
            if (shortUrlRepository.existsByCode(customCode)) {
                log.warn("Duplicate custom code detected: {}", customCode);
                throw new DuplicateCodeException(customCode);
            }
            if (topRankingCache.containsCode(customCode)) {
                log.warn("Duplicate custom code detected in cache: {}", customCode);
                throw new DuplicateCodeException(customCode);
            }
            final ShortUrl candidate = new ShortUrl(originalUrl, customCode, Instant.now());
            try {
                final ShortUrl saved = shortUrlRepository.saveAndFlush(candidate);
                log.info("Shorten created with custom code: {} -> {}", saved.getCode(), saved.getOriginalUrl());
                return saved;
            } catch (DataIntegrityViolationException e) {
                // Corrida entre instâncias: mapeia para 409
                log.warn("DataIntegrityViolation on custom code {}. Mapping to DuplicateCodeException.", customCode, e);
                throw new DuplicateCodeException(customCode);
            }
        }

        final int maxAttempts = 5;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            final String code = generateRandomCode();
            log.debug("Attempt {} generating random code: {}", attempt + 1, code);
            if (topRankingCache.containsCode(code)) {
                log.debug("Generated code {} is present in cache; retrying", code);
                continue;
            }
            final ShortUrl candidate = new ShortUrl(originalUrl, code, Instant.now());
            try {
                final ShortUrl saved = shortUrlRepository.saveAndFlush(candidate);
                log.info("Shorten created with generated code: {} -> {}", saved.getCode(), saved.getOriginalUrl());
                return saved;
            } catch (DataIntegrityViolationException e) {
                // Colisão de unicidade: tenta novamente com outro código
                log.warn("Collision detected for code {}. Retrying.", code);
            }
        }
        log.error("Failed to generate a unique code after {} attempts", maxAttempts);
        throw new IllegalStateException("Não foi possível gerar um código único após múltiplas tentativas");
    }

    public ShortUrl getByCode(String code) {
        log.debug("Fetching ShortUrl by code: {}", code);
        // Tenta pegar a entidade diretamente do Top-5
        ShortUrl topEntity = topRankingCache.getEntity(code);
        if (topEntity != null) {
            return topEntity;
        }
        // Fallback: busca no banco
        return shortUrlRepository.findByCode(code).orElse(null);
    }

    @Transactional
    public void registerAccess(ShortUrl shortUrl, String userAgent, String referer) {
        log.info("Registering access: code={}, userAgent={}, referer={}", shortUrl.getCode(), safe(userAgent), safe(referer));
        ShortUrlAccess access = new ShortUrlAccess(shortUrl, Instant.now(), userAgent, referer);
        shortUrlAccessRepository.save(access);
        try {
            topRankingCache.onAccess(shortUrl);
        } catch (Exception e) {
            log.warn("Failed to update top ranking cache on access for code={}: {}", shortUrl.getCode(), e.getMessage());
        }
    }

    public List<RankingItem> ranking() {
        log.info("Fetching ranking list (top-100 from cache)");
        List<RankingItem> top = topRankingCache.getTop();
        if (top.isEmpty()) {
            return shortUrlRepository.findRanking().stream().limit(100).toList();
        }
        return top;
    }

    public Page<StatsResponse> listStats(Pageable pageable) {
        log.info("Listing stats page: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return shortUrlRepository.findAllStats(pageable);
    }

    private String generateRandomCode() {
        return random.ints(CODE_LENGTH, 0, ALPHABET.length())
                .mapToObj(ALPHABET::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    private String safe(String s) {
        if (s == null) return null;
        final int max = 200;
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}