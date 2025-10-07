package br.corp.shortener.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "short_urls",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_code_original", columnNames = {"code", "original_url"})
    }
)
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 5)
    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ShortUrl() {
        // Construtor padrão para JPA
    }

    public ShortUrl(String originalUrl, String code, Instant createdAt) {
        this.originalUrl = originalUrl;
        this.code = code;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}