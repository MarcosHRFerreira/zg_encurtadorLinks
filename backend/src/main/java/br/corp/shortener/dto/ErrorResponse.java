package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(description = "Tipo do erro apresentado", example = "URL não encontrada") String error,
        @Schema(description = "Mensagem descritiva do erro", example = "O código informado não existe") String message
) {}