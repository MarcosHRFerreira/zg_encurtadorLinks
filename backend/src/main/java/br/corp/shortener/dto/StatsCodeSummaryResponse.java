package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record StatsCodeSummaryResponse(
        @Schema(description = "Código da URL encurtada", example = "ABCDE") String code,
        @Schema(description = "URL original") String originalUrl,
        @Schema(description = "Total de acessos para o código") Long totalHits,
        @Schema(description = "Total de acessos nos últimos 7 dias para o código") Long last7DaysHits,
        @Schema(description = "Acessos por dia (últimos 7) para o código") List<DayHits> daily
) {}