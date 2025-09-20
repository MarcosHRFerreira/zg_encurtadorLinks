package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StatsResponse(
        @Schema(description = "Código da URL encurtada", example = "ABCDE") String code,
        @Schema(description = "URL original") String originalUrl,
        @Schema(description = "Total de acessos", example = "42") Long hits
) {}