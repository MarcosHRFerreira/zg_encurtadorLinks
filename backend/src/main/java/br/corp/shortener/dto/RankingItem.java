package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RankingItem(
        @Schema(description = "Código da URL encurtada", example = "ABCDE") String code,
        @Schema(description = "URL original", example = "https://example.com/page") String originalUrl,
        @Schema(description = "Total de acessos", example = "42") Long hits
) {}