package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record DayHits(
        @Schema(description = "Data (UTC) do dia") LocalDate date,
        @Schema(description = "Total de acessos no dia", example = "7") Long hits
) {}