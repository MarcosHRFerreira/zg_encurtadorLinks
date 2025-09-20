package br.corp.shortener.repositories;

import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.entities.ShortUrlAccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlAccessRepository extends JpaRepository<ShortUrlAccess, Long> {
    long countByShortUrl(ShortUrl shortUrl);
}