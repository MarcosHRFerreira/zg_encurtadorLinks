package br.corp.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ShortenRequest {

    @Schema(description = "URL original a ser encurtada", example = "https://www.google.com/")
    @NotBlank(message = "url é obrigatória")
    private String url;

    @Schema(description = "Código customizado opcional com 5 caracteres alfanuméricos", example = "ABCDE")
    @Pattern(regexp = "^[A-Za-z0-9]{5}$", message = "code deve ter exatamente 5 caracteres alfanuméricos")
    private String code;

    public ShortenRequest() {
    }

    public ShortenRequest(String url, String code) {
        this.url = url;
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}