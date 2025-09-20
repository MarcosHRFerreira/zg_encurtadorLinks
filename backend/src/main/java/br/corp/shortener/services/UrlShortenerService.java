package br.corp.shortener.services;

import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import br.corp.shortener.exceptions.DuplicateCodeException;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.repositories.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
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
        String code = customCode != null ? customCode : generateUniqueCode();

        if (shortUrlRepository.existsByCode(code)) {
            throw new DuplicateCodeException(code);
        }

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(originalUrl)
                .code(code)
                .createdAt(Instant.now())
                .build();
        return shortUrlRepository.save(shortUrl);
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

    private String generateUniqueCode() {
        String code;
        do {
            code = random.ints(CODE_LENGTH, 0, ALPHABET.length())
                    .mapToObj(ALPHABET::charAt)
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
        } while (shortUrlRepository.existsByCode(code));
        return code;
    }
}