package br.corp.shortener.controllers;

import br.corp.shortener.dto.ErrorResponse;
import br.corp.shortener.dto.ShortenRequest;
import br.corp.shortener.dto.ShortenResponse;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.services.UrlShortenerService;
import br.corp.shortener.dto.ValidationErrorResponse;
import br.corp.shortener.dto.RankingItem;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@Tag(name = "Links", description = "Endpoints para encurtamento, redirecionamento e ranking")
public class LinkController {

    private final UrlShortenerService service;

    private static final Logger log = LoggerFactory.getLogger(LinkController.class);

    public LinkController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    @Operation(
            summary = "Encurtar URL",
            description = "Cria uma URL encurtada a partir da URL original",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShortenRequest.class),
                            examples = {
                                    @ExampleObject(name = "Mínimo", value = "{\"url\":\"https://www.google.com/\"}"),
                                    @ExampleObject(name = "Com código customizado", value = "{\"url\":\"https://www.google.com/\", \"code\":\"ABCDE\"}")
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado", content = @Content(schema = @Schema(implementation = ShortenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Código em uso", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        log.info("Shorten endpoint called: url={}, customCodeProvided={}", request.url(), request.code() != null);
        ShortUrl shortUrl = service.shorten(request.url(), request.code());
        String fullShortUrl;
        try {
            fullShortUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromCurrentRequestUri()
                    .replacePath("/" + shortUrl.getCode())
                    .build()
                    .toUriString();
        } catch (IllegalStateException _e) {
            // Sem contexto de requisição (ex.: testes unitários): usa caminho relativo
            fullShortUrl = "/" + shortUrl.getCode();
        }
        ShortenResponse body = new ShortenResponse(shortUrl.getId(), shortUrl.getOriginalUrl(), shortUrl.getCode(), shortUrl.getCreatedAt(), fullShortUrl);
        log.info("Shorten created: code={}, location=/{}", shortUrl.getCode(), shortUrl.getCode());
        return ResponseEntity.created(URI.create("/" + shortUrl.getCode())).body(body);
    }

    @GetMapping("/ranking")
    @Operation(summary = "Ranking de códigos mais acessados", description = "Lista as URLs mais acessadas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RankingItem.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<RankingItem> ranking() {
        log.info("Ranking endpoint called");
        return service.ranking();
    }

    @GetMapping("/{code:[A-Za-z0-9]{5}}")
    @Operation(summary = "Redirecionar para URL original")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirecionado"),
            @ApiResponse(responseCode = "404", description = "URL não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> redirect(@PathVariable("code") String code,
                                      @RequestHeader(value = "User-Agent", required = false) String ua,
                                      @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            log.info("Redirect requested: code={}", code);
            ShortUrl su = service.getByCode(code);
            if (su == null) {
                log.warn("Redirect failed: code not found={}", code);
                ErrorResponse errorResponse = new ErrorResponse("URL não encontrada", "O código informado não existe");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            service.registerAccess(su, ua, referer);
            log.info("Redirecting code={} to {}", code, su.getOriginalUrl());

            // Usa API segura para definir Location evitando cabeçalhos manuais
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(su.getOriginalUrl()))
                    .build();
        } catch (Exception e) {
            log.error("Redirect error for code={}: {}", code, e.getMessage(), e);
            ErrorResponse errorResponse = new ErrorResponse("Erro interno", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}