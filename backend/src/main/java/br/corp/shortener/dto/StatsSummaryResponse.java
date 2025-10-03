package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record StatsSummaryResponse(
        @Schema(description = "Total de acessos") Long totalHits,
        @Schema(description = "Total de acessos nos últimos 7 dias") Long last7DaysHits,
        @Schema(description = "Acessos por dia (últimos 7)") List<DayHits> daily
) {}