package br.corp.shortener.services;

import br.corp.shortener.dto.RankingItem;
import br.corp.shortener.entities.ShortUrl;
import br.corp.shortener.repositories.ShortUrlAccessRepository;
import br.corp.shortener.repositories.ShortUrlRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TopRankingCache {

    private static final Logger log = LoggerFactory.getLogger(TopRankingCache.class);

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlAccessRepository accessRepository;

    // Mantém mapping code -> hits para os TOP N (Top-100)
    private final Map<String, Long> hitsByCode = new LinkedHashMap<>();
    // Mantém a entidade ShortUrl para códigos do TOP N
    private final Map<String, ShortUrl> entityByCode = new LinkedHashMap<>();

    private static final int TOP_LIMIT = 100;

    public TopRankingCache(ShortUrlRepository shortUrlRepository, ShortUrlAccessRepository accessRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.accessRepository = accessRepository;
    }

    @PostConstruct
    public void preload() {
        try {
            log.info("Preloading top-{} ranking cache", TOP_LIMIT);
            List<RankingItem> all = shortUrlRepository.findRanking();
            List<RankingItem> topN = all.stream().sorted(Comparator.comparingLong(r -> -r.hits())).limit(TOP_LIMIT).collect(Collectors.toList());
            hitsByCode.clear();
            entityByCode.clear();
            for (RankingItem item : topN) {
                hitsByCode.put(item.code(), item.hits());
                try {
                    ShortUrl su = shortUrlRepository.findByCode(item.code()).orElse(null);
                    if (su != null) {
                        entityByCode.put(item.code(), su);
                    } else {
                        log.debug("Top preload: entity not found for code {}", item.code());
                    }
                } catch (Exception e) {
                    log.debug("Failed to load entity for code {} during preload: {}", item.code(), e.getMessage());
                }
            }
            log.info("Top-{} cache loaded: {}", TOP_LIMIT, hitsByCode.keySet());
        } catch (Exception e) {
            log.warn("Failed to preload ranking cache: {}", e.getMessage(), e);
            hitsByCode.clear();
            entityByCode.clear();
        }
    }

    public List<RankingItem> getTop() {
        // Retorna em ordem decrescente de hits
        return hitsByCode.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new RankingItem(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean containsCode(String code) {
        return hitsByCode.containsKey(code);
    }

    public ShortUrl getEntity(String code) {
        if (code == null) return null;
        return entityByCode.get(code);
    }

    public Long getHits(String code) {
        if (code == null) return null;
        return hitsByCode.get(code);
    }

    public void onAccess(ShortUrl su) {
        String code = su.getCode();
        Long current = hitsByCode.get(code);
        if (current != null) {
            hitsByCode.put(code, current + 1);
            // Garante que a entidade esteja disponível
            entityByCode.putIfAbsent(code, su);
            return;
        }
        // Não está no cache: buscar contagem atual no banco
        long dbCount = accessRepository.countByShortUrl(su);
        if (hitsByCode.size() < TOP_LIMIT) {
            hitsByCode.put(code, dbCount);
            entityByCode.put(code, su);
        } else {
            // Verificar se supera o menor do top-N
            Optional<Map.Entry<String, Long>> minEntryOpt = hitsByCode.entrySet().stream().min(Map.Entry.comparingByValue());
            if (minEntryOpt.isPresent() && dbCount > minEntryOpt.get().getValue()) {
                String minKey = minEntryOpt.get().getKey();
                hitsByCode.remove(minKey);
                entityByCode.remove(minKey);
                hitsByCode.put(code, dbCount);
                entityByCode.put(code, su);
            }
        }
    }
}