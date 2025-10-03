package br.corp.shortener.controllers;

import br.corp.shortener.dto.ErrorResponse;
import br.corp.shortener.dto.DayHits;
import br.corp.shortener.dto.StatsResponse;
import br.corp.shortener.dto.StatsSummaryResponse;
import br.corp.shortener.dto.StatsCodeSummaryResponse;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.services.UrlShortenerService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@Tag(name = "Estatísticas", description = "Endpoints para consulta de estatísticas")
public class StatsController {

    private final UrlShortenerService service;
    private final ShortUrlAccessRepository accessRepository;
    private static final Logger log = LoggerFactory.getLogger(StatsController.class);

    public StatsController(UrlShortenerService service, ShortUrlAccessRepository accessRepository) {
        this.service = service;
        this.accessRepository = accessRepository;
    }

    @GetMapping("/stats/{code:[A-Za-z0-9]{5}}")
    @Operation(summary = "Obter estatísticas da URL", description = "Retorna o código, URL original e total de acessos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StatsResponse.class))),
            @ApiResponse(responseCode = "404", description = "URL não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> stats(@PathVariable("code") String code) {
        log.info("Stats requested for code={}", code);
        ShortUrl su = service.getByCode(code);
        if (su == null) {
            log.warn("Stats not found for code={}", code);
            ErrorResponse errorResponse = new ErrorResponse("URL não encontrada", "O código informado não existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        long hits = accessRepository.countByShortUrl(su);
        log.info("Stats found: code={}, hits={}", su.getCode(), hits);
        StatsResponse resp = new StatsResponse(su.getCode(), su.getOriginalUrl(), hits);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/stats")
    @Operation(summary = "Listar estatísticas paginadas", description = "Retorna todas as estatísticas em uma lista paginada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StatsResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<StatsResponse>> statsPage(Pageable pageable) {
        log.info("Stats page requested: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<StatsResponse> page = service.listStats(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/stats/summary")
    @Operation(summary = "Resumo de estatísticas", description = "Total de acessos, últimos 7 dias e acessos por dia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StatsSummaryResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<StatsSummaryResponse> summary() {
        log.info("Stats summary requested");
        long total = accessRepository.count();

        Instant now = Instant.now();
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);
        long last7 = accessRepository.countByAccessedAtAfter(cutoff);

        ZoneId zone = ZoneOffset.UTC;
        LocalDate today = LocalDate.now(zone);
        List<DayHits> daily = new ArrayList<>();

        // Últimos 7 dias em ordem cronológica (antigo -> recente)
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Instant start = day.atStartOfDay(zone).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
            long hits = accessRepository.countByAccessedAtBetween(start, end);
            if (hits > 0) {
                daily.add(new DayHits(day, hits));
            }
        }

        StatsSummaryResponse resp = new StatsSummaryResponse(total, last7, daily);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/stats/{code:[A-Za-z0-9]{5}}/summary")
    @Operation(summary = "Resumo de estatísticas por código", description = "Total de acessos, últimos 7 dias e acessos por dia para um código")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = StatsCodeSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "URL não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> summaryByCode(@PathVariable("code") String code) {
        log.info("Stats code summary requested for code={}", code);
        ShortUrl su = service.getByCode(code);
        if (su == null) {
            log.warn("Stats not found for code={}", code);
            ErrorResponse errorResponse = new ErrorResponse("URL não encontrada", "O código informado não existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        long totalHits = accessRepository.countByShortUrl(su);
        long last7DaysHits = accessRepository.countByShortUrlAndAccessedAtAfter(su, cutoff);

        ZoneId zone = ZoneOffset.UTC;
        LocalDate today = LocalDate.now(zone);
        List<DayHits> daily = new ArrayList<>();

        // Últimos 7 dias em ordem cronológica (antigo -> recente)
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Instant start = day.atStartOfDay(zone).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
            long hits = accessRepository.countByShortUrlAndAccessedAtBetween(su, start, end);
            if (hits > 0) {
                daily.add(new DayHits(day, hits));
            }
        }

        StatsCodeSummaryResponse resp = new StatsCodeSummaryResponse(su.getCode(), su.getOriginalUrl(), totalHits, last7DaysHits, daily);
        return ResponseEntity.ok(resp);
    }
}