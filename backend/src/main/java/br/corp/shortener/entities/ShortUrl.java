package br.corp.shortener.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "short_urls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}