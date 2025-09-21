package br.corp.shortener.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "short_url_accesses")
public class ShortUrlAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", nullable = false)
    private ShortUrl shortUrl;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "referer")
    private String referer;

    protected ShortUrlAccess() {
        // Construtor padrão para JPA
    }

    public ShortUrlAccess(ShortUrl shortUrl, Instant accessedAt, String userAgent, String referer) {
        this.shortUrl = shortUrl;
        this.accessedAt = accessedAt;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ShortUrl getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(ShortUrl shortUrl) {
        this.shortUrl = shortUrl;
    }

    public Instant getAccessedAt() {
        return accessedAt;
    }

    public void setAccessedAt(Instant accessedAt) {
        this.accessedAt = accessedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }
}