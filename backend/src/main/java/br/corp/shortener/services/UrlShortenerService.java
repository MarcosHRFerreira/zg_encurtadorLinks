package br.corp.shortener.services;

import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.dto.StatsResponse;
import br.corp.shortener.dto.StatsSummaryResponse;
import br.corp.shortener.dto.StatsCodeSummaryResponse;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import br.corp.shortener.exceptions.DuplicateCodeException;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.repositories.ShortUrlRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
            // Primeiro valida no cache
            if (topRankingCache.containsCode(code)) {
                log.debug("Generated code {} is present in cache; retrying", code);
                continue;
            }
            // Se não estiver no cache, valida existência no banco
            if (shortUrlRepository.existsByCode(code)) {
                log.debug("Generated code {} already exists in database; retrying", code);
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
        // Atualiza o cache apenas após o commit da transação para evitar inconsistência em caso de rollback
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        topRankingCache.onAccess(shortUrl);
                    } catch (Exception e) {
                        log.warn("Failed to update top ranking cache on access after commit for code={}: {}", shortUrl.getCode(), e.getMessage());
                    }
                }
            });
        } else {
            // Sem contexto transacional: atualiza imediatamente
            try {
                topRankingCache.onAccess(shortUrl);
            } catch (Exception e) {
                log.warn("Failed to update top ranking cache on access for code={}: {}", shortUrl.getCode(), e.getMessage());
            }
        }
    }

    public List<RankingItem> ranking() {
        log.info("Fetching ranking list exclusively from cache (top-100)");
        return topRankingCache.getTop();
    }

    public Page<StatsResponse> listStats(Pageable pageable) {
        log.info("Listing stats page: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return shortUrlRepository.findAllStats(pageable);
    }

    /**
     * Calcula estatísticas agregadas globais (total, últimos 7 dias e diário).
     */
    public StatsSummaryResponse getStatsSummary() {
        log.info("Computing global stats summary");
        long total = shortUrlAccessRepository.count();

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(7 * 24 * 60 * 60);
        long last7 = shortUrlAccessRepository.countByAccessedAtAfter(cutoff);

        ZoneId zone = ZoneOffset.UTC;
        LocalDate today = LocalDate.now(zone);
        List<br.corp.shortener.dto.DayHits> daily = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Instant start = day.atStartOfDay(zone).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
            long hits = shortUrlAccessRepository.countByAccessedAtBetween(start, end);
            if (hits > 0) {
                daily.add(new br.corp.shortener.dto.DayHits(day, hits));
            }
        }

        return new StatsSummaryResponse(total, last7, daily);
    }

    /**
     * Calcula estatísticas para um código específico.
     * Retorna null se o código não existir.
     */
    public StatsResponse getStats(String code) {
        ShortUrl su = getByCode(code);
        if (su == null) return null;

        Long cachedHits = topRankingCache.getHits(su.getCode());
        long hits = cachedHits != null ? cachedHits : shortUrlAccessRepository.countByShortUrl(su);
        return new StatsResponse(su.getCode(), su.getOriginalUrl(), hits);
    }

    /**
     * Calcula resumo de estatísticas para um código específico.
     * Retorna null se o código não existir.
     */
    public StatsCodeSummaryResponse getStatsSummaryByCode(String code) {
        ShortUrl su = getByCode(code);
        if (su == null) return null;

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(7 * 24 * 60 * 60);

        Long cachedHits = topRankingCache.getHits(su.getCode());
        long totalHits = cachedHits != null ? cachedHits : shortUrlAccessRepository.countByShortUrl(su);
        long last7DaysHits = shortUrlAccessRepository.countByShortUrlAndAccessedAtAfter(su, cutoff);

        ZoneId zone = ZoneOffset.UTC;
        LocalDate today = LocalDate.now(zone);
        List<br.corp.shortener.dto.DayHits> daily = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Instant start = day.atStartOfDay(zone).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
            long hits = shortUrlAccessRepository.countByShortUrlAndAccessedAtBetween(su, start, end);
            if (hits > 0) {
                daily.add(new br.corp.shortener.dto.DayHits(day, hits));
            }
        }

        return new StatsCodeSummaryResponse(su.getCode(), su.getOriginalUrl(), totalHits, last7DaysHits, daily);
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