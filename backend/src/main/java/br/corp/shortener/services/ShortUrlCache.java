package br.corp.shortener.services;

import br.corp.shortener.entities.ShortUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache simples para idempotência de ShortUrl.
 * Mantém índices por código, por URL e pelo par (code|originalUrl).
 */
@Component
public class ShortUrlCache {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlCache.class);

    private final ConcurrentMap<String, ShortUrl> byCode = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ShortUrl> byUrl = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ShortUrl> byPair = new ConcurrentHashMap<>();

    public Optional<ShortUrl> getByPair(String code, String originalUrl) {
        if (code == null || originalUrl == null) return Optional.empty();
        return Optional.ofNullable(byPair.get(pairKey(code, originalUrl)));
    }

    public Optional<ShortUrl> getByUrl(String originalUrl) {
        if (originalUrl == null) return Optional.empty();
        return Optional.ofNullable(byUrl.get(originalUrl));
    }

    public boolean containsCode(String code) {
        if (code == null) return false;
        return byCode.containsKey(code);
    }

    public ShortUrl put(ShortUrl su) {
        if (su == null) return null;
        try {
            byCode.put(su.getCode(), su);
            byUrl.put(su.getOriginalUrl(), su);
            byPair.put(pairKey(su.getCode(), su.getOriginalUrl()), su);
        } catch (Exception e) {
            log.debug("Failed to put ShortUrl in cache: {}", e.getMessage());
        }
        return su;
    }

    private String pairKey(String code, String originalUrl) {
        return code + "|" + originalUrl;
    }
}