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
    private ShortUrlCache shortUrlCache; // opcional

    public UrlShortenerService(ShortUrlRepository shortUrlRepository, ShortUrlAccessRepository shortUrlAccessRepository, TopRankingCache topRankingCache) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlAccessRepository = shortUrlAccessRepository;
        this.topRankingCache = topRankingCache;
    }

    // Injeta cache de ShortUrl opcionalmente para não quebrar testes/unitários
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setShortUrlCache(ShortUrlCache shortUrlCache) {
        this.shortUrlCache = shortUrlCache;
    }

    @Transactional
    public ShortUrl shorten(String originalUrl, String customCode) {
        log.info("Shorten requested: originalUrl={}, customCodeProvided={}", originalUrl, customCode != null);
        // Valida e normaliza URL de entrada para mitigar open redirect/CRLF e tamanhos excessivos
        final String safeOriginalUrl = validateAndNormalizeUrl(originalUrl);
        if (customCode != null) {
            // Idempotência: se o par (code, originalUrl) já existe, retorna como incluído
            ShortUrl existingPair = shortUrlCache != null
                    ? shortUrlCache.getByPair(customCode, safeOriginalUrl).orElse(null)
                    : null;
            if (existingPair == null) {
                existingPair = shortUrlRepository.findByCodeAndOriginalUrl(customCode, safeOriginalUrl).orElse(null);
                if (existingPair != null) {
                    putAfterCommit(existingPair);
                }
            }
            if (existingPair != null) {
                log.info("Existing pair found for custom code: {} -> {}. Returning as created.", customCode, safeOriginalUrl);
                return existingPair;
            }

            // Colisão real: código em uso para outra URL
            boolean codeInUse = topRankingCache.containsCode(customCode)
                    || (shortUrlCache != null && shortUrlCache.containsCode(customCode))
                    || shortUrlRepository.existsByCode(customCode);
            if (codeInUse) {
                log.warn("Duplicate custom code detected (different URL): {}", customCode);
                throw new DuplicateCodeException(customCode);
            }

            final ShortUrl candidate = new ShortUrl(safeOriginalUrl, customCode, Instant.now());
            try {
                final ShortUrl saved = shortUrlRepository.saveAndFlush(candidate);
                log.info("Shorten created with custom code: {} -> {}", saved.getCode(), saved.getOriginalUrl());
                putAfterCommit(saved);
                return saved;
            } catch (DataIntegrityViolationException e) {
                // Corrida entre instâncias: tenta recuperar o par e retornar como criado
                ShortUrl raced = shortUrlRepository.findByCodeAndOriginalUrl(customCode, safeOriginalUrl).orElse(null);
                if (raced != null) {
                    log.info("Race detected and resolved for custom code: {} -> {}. Returning existing.", customCode, safeOriginalUrl);
                    putAfterCommit(raced);
                    return raced;
                }
                log.warn("DataIntegrityViolation on custom code {} without matching pair.", customCode, e);
                throw new DuplicateCodeException(customCode);
            }
        }

        // Idempotência: sem código, retorna existente por URL se já houver
        ShortUrl existingByUrl = shortUrlCache != null
                ? shortUrlCache.getByUrl(safeOriginalUrl).orElse(null)
                : null;
        if (existingByUrl == null) {
            existingByUrl = shortUrlRepository.findFirstByOriginalUrlOrderByCreatedAtDesc(safeOriginalUrl).orElse(null);
            if (existingByUrl != null) {
                putAfterCommit(existingByUrl);
            }
        }
        if (existingByUrl != null) {
            log.info("Existing entry found for originalUrl {}. Returning as created.", safeOriginalUrl);
            return existingByUrl;
        }

        final int maxAttempts = 5;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            final String code = generateRandomCode();
            log.debug("Attempt {} generating random code: {}", attempt + 1, code);
            // Primeiro valida no cache/TopRanking
            boolean inCache = topRankingCache.containsCode(code)
                    || (shortUrlCache != null && shortUrlCache.containsCode(code));
            if (inCache) {
                log.debug("Generated code {} is present in cache; retrying", code);
                continue;
            }
            // Fallback: valida existência no banco
            if (shortUrlRepository.existsByCode(code)) {
                log.debug("Generated code {} already exists in database; retrying", code);
                continue;
            }
            final ShortUrl candidate = new ShortUrl(safeOriginalUrl, code, Instant.now());
            try {
                final ShortUrl saved = shortUrlRepository.saveAndFlush(candidate);
                log.info("Shorten created with generated code: {} -> {}", saved.getCode(), saved.getOriginalUrl());
                putAfterCommit(saved);
                return saved;
            } catch (DataIntegrityViolationException e) {
                // Colisão de unicidade: se o par existir, retorna como criado; senão tenta outro código
                ShortUrl raced = shortUrlRepository.findByCodeAndOriginalUrl(code, safeOriginalUrl).orElse(null);
                if (raced != null) {
                    log.info("Race detected and resolved for generated code: {} -> {}. Returning existing.", code, safeOriginalUrl);
                    putAfterCommit(raced);
                    return raced;
                }
                log.warn("Collision detected for code {} without matching pair. Retrying.", code);
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

    private void putAfterCommit(ShortUrl su) {
        if (shortUrlCache == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        shortUrlCache.put(su);
                    } catch (Exception e) {
                        log.warn("Failed to update ShortUrl cache after commit for code={}: {}", su.getCode(), e.getMessage());
                    }
                }
            });
        } else {
            try {
                shortUrlCache.put(su);
            } catch (Exception e) {
                log.warn("Failed to update ShortUrl cache for code={}: {}", su.getCode(), e.getMessage());
            }
        }
    }

    private String safe(String s) {
        if (s == null) return null;
        final int max = 200;
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    /**
     * Valida e normaliza a URL original.
     * Regras:
     * - Remove espaços em branco
     * - Tamanho máximo 2048 caracteres
     * - Proíbe CR/LF para evitar HTTP Response Splitting
     * - Exige esquemas http/https e host presente
     */
    private String validateAndNormalizeUrl(String originalUrl) {
        if (originalUrl == null) {
            throw new IllegalArgumentException("url é obrigatória");
        }
        String trimmed = originalUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("url é obrigatória");
        }
        if (trimmed.length() > 2048) {
            throw new IllegalArgumentException("url muito longa (máx. 2048)");
        }
        if (trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("url inválida");
        }
        java.net.URI uri;
        try {
            uri = java.net.URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("url inválida");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("url deve usar http ou https");
        }
        // Em alguns casos, URI permite sem host; exigimos host para evitar redirecionamentos estranhos
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("url inválida (host ausente)");
        }
        // Normaliza removendo fragmento e mantendo query
        try {
            java.net.URI normalized = new java.net.URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null // sem fragmento
            );
            return normalized.toString();
        } catch (Exception _e) {
            // Fallback: retorna original aparado
            return trimmed;
        }
    }
}