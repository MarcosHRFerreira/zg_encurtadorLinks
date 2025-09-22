package br.corp.shortener.repositories;

import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.dto.StatsResponse;
import br.corp.shortener.entities.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT su FROM ShortUrl su WHERE su.originalUrl = :originalUrl")
    List<ShortUrl> findAllByOriginalUrl(@Param("originalUrl") String originalUrl);

    @Query("SELECT new br.corp.shortener.dto.RankingItem(su.code, COUNT(a)) FROM ShortUrlAccess a JOIN a.shortUrl su GROUP BY su ORDER BY COUNT(a) DESC")
    List<RankingItem> findRanking();

    @Query(value = "SELECT new br.corp.shortener.dto.StatsResponse(su.code, su.originalUrl, COUNT(a)) FROM ShortUrl su LEFT JOIN ShortUrlAccess a ON a.shortUrl = su GROUP BY su.id, su.code, su.originalUrl ORDER BY su.createdAt DESC", countQuery = "SELECT COUNT(su) FROM ShortUrl su")
    Page<StatsResponse> findAllStats(Pageable pageable);
}