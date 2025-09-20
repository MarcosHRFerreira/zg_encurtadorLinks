package br.corp.shortener.controllers;

import br.corp.shortener.dto.ErrorResponse;
import br.corp.shortener.dto.StatsResponse;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.services.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(name = "Estatísticas", description = "Endpoints para consulta de estatísticas")
public class StatsController {

    private final UrlShortenerService service;
    private final ShortUrlAccessRepository accessRepository;

    @GetMapping("/stats/{code:[A-Za-z0-9]{5}}")
    @Operation(summary = "Obter estatísticas da URL", description = "Retorna o código, URL original e total de acessos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StatsResponse.class))),
            @ApiResponse(responseCode = "404", description = "URL não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> stats(@PathVariable("code") String code) {
        ShortUrl su = service.getByCode(code);
        if (su == null) {
            ErrorResponse errorResponse = new ErrorResponse("URL não encontrada", "O código informado não existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        long hits = accessRepository.countByShortUrl(su);
        StatsResponse resp = new StatsResponse(su.getCode(), su.getOriginalUrl(), hits);
        return ResponseEntity.ok(resp);
    }
}