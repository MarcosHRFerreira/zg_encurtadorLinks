package br.corp.shortener.repositories;

import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface ShortUrlAccessRepository extends JpaRepository<ShortUrlAccess, Long> {
    long countByShortUrl(ShortUrl shortUrl);
    long countByAccessedAtAfter(Instant cutoff);
    long countByAccessedAtBetween(Instant start, Instant end);
    long countByShortUrlAndAccessedAtAfter(ShortUrl shortUrl, Instant cutoff);
    long countByShortUrlAndAccessedAtBetween(ShortUrl shortUrl, Instant start, Instant end);
}