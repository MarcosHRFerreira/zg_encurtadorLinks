package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ShortenResponse(
        @Schema(description = "Identificador interno", example = "1") Long id,
        @Schema(description = "URL original") String originalUrl,
        @Schema(description = "Código da URL encurtada", example = "ABCDE") String code,
        @Schema(description = "Data de criação em UTC") Instant createdAt
) {}