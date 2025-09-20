package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record ValidationErrorResponse(
        @Schema(description = "Tipo do erro apresentado", example = "Erro de validação") String error,
        @Schema(description = "Detalhes de erro por campo") Map<String, String> details
) {}