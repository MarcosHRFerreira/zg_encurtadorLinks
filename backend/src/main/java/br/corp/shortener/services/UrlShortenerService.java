package br.corp.shortener.services;

import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import br.corp.shortener.exceptions.DuplicateCodeException;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.repositories.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 5;
    private final Random random = new SecureRandom();

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlAccessRepository shortUrlAccessRepository;

    @Transactional
    public ShortUrl shorten(String originalUrl, String customCode) {
        if (customCode != null) {
            if (shortUrlRepository.existsByCode(customCode)) {
                throw new DuplicateCodeException(customCode);
            }
            final ShortUrl candidate = ShortUrl.builder()
                    .originalUrl(originalUrl)
                    .code(customCode)
                    .createdAt(Instant.now())
                    .build();
            try {
                return shortUrlRepository.saveAndFlush(candidate);
            } catch (DataIntegrityViolationException e) {
                // Corrida entre instâncias: mapeia para 409
                throw new DuplicateCodeException(customCode);
            }
        }

        final int maxAttempts = 5;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            final String code = generateRandomCode();
            final ShortUrl candidate = ShortUrl.builder()
                    .originalUrl(originalUrl)
                    .code(code)
                    .createdAt(Instant.now())
                    .build();
            try {
                return shortUrlRepository.saveAndFlush(candidate);
            } catch (DataIntegrityViolationException e) {
                // Colisão de unicidade: tenta novamente com outro código
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código único após múltiplas tentativas");
    }

    public ShortUrl getByCode(String code) {
        return shortUrlRepository.findByCode(code).orElse(null);
    }

    @Transactional
    public void registerAccess(ShortUrl shortUrl, String userAgent, String referer) {
        ShortUrlAccess access = ShortUrlAccess.builder()
                .shortUrl(shortUrl)
                .accessedAt(Instant.now())
                .userAgent(userAgent)
                .referer(referer)
                .build();
        shortUrlAccessRepository.save(access);
    }

    public List<RankingItem> ranking() {
        return shortUrlRepository.findRanking();
    }

    private String generateRandomCode() {
        return random.ints(CODE_LENGTH, 0, ALPHABET.length())
                .mapToObj(ALPHABET::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}