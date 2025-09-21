package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShortenRequest(
        @Schema(description = "URL original a ser encurtada", example = "https://www.google.com/")
        @NotBlank(message = "url é obrigatória") String url,

        @Schema(description = "Código customizado opcional com 5 caracteres alfanuméricos", example = "ABCDE")
        @Pattern(regexp = "^[A-Za-z0-9]{5}$", message = "code deve ter exatamente 5 caracteres alfanuméricos") String code
) {}