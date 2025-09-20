package br.corp.shortener.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "short_url_accesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}