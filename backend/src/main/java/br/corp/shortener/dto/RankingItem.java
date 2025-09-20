package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RankingItem(
        @Schema(description = "Código da URL encurtada", example = "ABCDE") String code,
        @Schema(description = "Total de acessos", example = "42") Long hits
) {}