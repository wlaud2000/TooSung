package com.project.toosung_back.domain.watchlist.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.NewsStockRepository;
import com.project.toosung_back.domain.watchlist.ai.SectorAnalysisPrompt;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistSectorResDTO;
import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import com.project.toosung_back.global.openai.client.OpenAiClient;
import com.project.toosung_back.global.openai.dto.OpenAiChatResponse;
import com.project.toosung_back.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorAnalysisService {

    private static final int MAX_NEWS_PER_SECTOR = 5;
    private static final String CACHE_KEY_PREFIX = "sector-analysis:";

    private final WatchlistRepository watchlistRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final NewsStockRepository newsStockRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final RedisUtil redisUtil;

    @Transactional(readOnly = true)
    public WatchlistSectorResDTO.SectorAnalysisList getSectorAnalysis(Long memberId) {
        String cacheKey = CACHE_KEY_PREFIX + memberId;

        if (redisUtil.hasKey(cacheKey)) {
            try {
                return objectMapper.readValue(redisUtil.get(cacheKey), WatchlistSectorResDTO.SectorAnalysisList.class);
            } catch (JsonProcessingException e) {
                redisUtil.delete(cacheKey);
            }
        }

        WatchlistSectorResDTO.SectorAnalysisList result = buildSectorAnalysis(memberId);
        cacheUntilMidnight(cacheKey, result);
        return result;
    }

    private WatchlistSectorResDTO.SectorAnalysisList buildSectorAnalysis(Long memberId) {
        List<Watchlist> watchlists = watchlistRepository.findByMember_IdOrderByPositionAsc(memberId)
                .stream().filter(w -> !w.isDeleted()).toList();

        if (watchlists.isEmpty()) {
            return WatchlistSectorResDTO.SectorAnalysisList.builder().sectors(List.of()).build();
        }

        // 섹터별 종목 그룹핑
        Map<String, List<Watchlist>> bySector = watchlists.stream()
                .filter(w -> w.getStock().getSector() != null && !w.getStock().getSector().isBlank())
                .collect(Collectors.groupingBy(w -> w.getStock().getSector()));

        if (bySector.isEmpty()) {
            return WatchlistSectorResDTO.SectorAnalysisList.builder().sectors(List.of()).build();
        }

        List<Long> allStockIds = watchlists.stream().map(w -> w.getStock().getId()).toList();
        LocalDateTime from = LocalDateTime.now().minusHours(12);

        // 종목별 감성 카운트
        Map<Long, Map<Sentiment, Integer>> sentimentMap = buildSentimentMap(allStockIds, from);

        // 최근 12시간 뉴스 전체 조회 후 종목→섹터 매핑
        List<NewsAnalysis> allNews = newsAnalysisRepository.findTodayByStockIds(
                allStockIds, from, PageRequest.of(0, 50));

        Map<Long, String> stockIdToSector = watchlists.stream()
                .filter(w -> w.getStock().getSector() != null)
                .collect(Collectors.toMap(w -> w.getStock().getId(), w -> w.getStock().getSector(), (a, b) -> a));

        // 뉴스를 섹터별로 분류
        Map<Long, Long> newsIdToStockId = buildNewsIdToStockId(
                allNews.stream().map(na -> na.getNews().getId()).toList());

        Map<String, List<NewsAnalysis>> sectorNewsMap = new LinkedHashMap<>();
        Set<Long> seenNewsIds = new HashSet<>();
        for (NewsAnalysis na : allNews) {
            if (!seenNewsIds.add(na.getNews().getId())) continue;
            Long stockId = newsIdToStockId.get(na.getNews().getId());
            if (stockId == null) continue;
            String sector = stockIdToSector.get(stockId);
            if (sector == null) continue;
            sectorNewsMap.computeIfAbsent(sector, k -> new ArrayList<>()).add(na);
        }

        // 섹터별 뉴스 MAX 제한
        sectorNewsMap.replaceAll((s, list) -> list.size() > MAX_NEWS_PER_SECTOR
                ? list.subList(0, MAX_NEWS_PER_SECTOR) : list);

        // AI 분석 생성 (뉴스 있는 섹터만)
        Map<String, String> aiAnalyses = Map.of();
        Map<String, List<NewsAnalysis>> withNews = sectorNewsMap.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!withNews.isEmpty()) {
            aiAnalyses = generateAiAnalyses(withNews);
        }

        // 최종 결과 조립
        final Map<String, String> finalAiAnalyses = aiAnalyses;
        List<WatchlistSectorResDTO.SectorAnalysis> sectors = bySector.entrySet().stream()
                .map(entry -> {
                    String sector = entry.getKey();
                    List<Long> stockIds = entry.getValue().stream().map(w -> w.getStock().getId()).toList();
                    List<String> stockNames = entry.getValue().stream().map(w -> w.getStock().getName()).toList();

                    int pos = 0, neg = 0, neu = 0;
                    for (Long sid : stockIds) {
                        Map<Sentiment, Integer> counts = sentimentMap.getOrDefault(sid, Map.of());
                        pos += counts.getOrDefault(Sentiment.POSITIVE, 0);
                        neg += counts.getOrDefault(Sentiment.NEGATIVE, 0);
                        neu += counts.getOrDefault(Sentiment.NEUTRAL, 0);
                    }

                    return WatchlistSectorResDTO.SectorAnalysis.builder()
                            .sector(sector)
                            .sentiment(determineSentiment(pos, neg, neu))
                            .analysis(finalAiAnalyses.getOrDefault(sector, "오늘 관련 뉴스가 없어요."))
                            .positiveCount(pos)
                            .negativeCount(neg)
                            .neutralCount(neu)
                            .stockNames(stockNames)
                            .build();
                })
                .sorted(Comparator.comparingInt(WatchlistSectorResDTO.SectorAnalysis::totalNews).reversed())
                .toList();

        return WatchlistSectorResDTO.SectorAnalysisList.builder().sectors(sectors).build();
    }

    private Map<Long, Map<Sentiment, Integer>> buildSentimentMap(List<Long> stockIds, LocalDateTime from) {
        Map<Long, Map<Sentiment, Integer>> result = new HashMap<>();
        for (Object[] row : newsAnalysisRepository.countSentimentByStockIds(stockIds, from)) {
            Long stockId = (Long) row[0];
            Sentiment sentiment = (Sentiment) row[1];
            int count = ((Number) row[2]).intValue();
            result.computeIfAbsent(stockId, k -> new HashMap<>()).put(sentiment, count);
        }
        return result;
    }

    private Map<Long, Long> buildNewsIdToStockId(List<Long> newsIds) {
        if (newsIds.isEmpty()) return Map.of();
        return newsStockRepository.findAllByNewsIdIn(newsIds).stream()
                .collect(Collectors.toMap(ns -> ns.getNews().getId(), ns -> ns.getStock().getId(), (a, b) -> a));
    }

    private Map<String, String> generateAiAnalyses(Map<String, List<NewsAnalysis>> sectorNewsMap) {
        try {
            OpenAiChatResponse response = openAiClient.chat(SectorAnalysisPrompt.build(sectorNewsMap));
            String rawJson = response.choices().get(0).message().content();
            JsonNode node = objectMapper.readTree(rawJson);

            Map<String, String> result = new HashMap<>();
            node.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText()));
            return result;
        } catch (Exception e) {
            log.warn("[SectorAnalysisService] AI 분석 실패: {}", e.getMessage());
            return Map.of();
        }
    }

    private String determineSentiment(int pos, int neg, int neu) {
        int total = pos + neg + neu;
        if (total == 0) return "NEUTRAL";
        double posRate = (double) pos / total;
        double negRate = (double) neg / total;
        if (posRate >= 0.5) return "BULLISH";
        if (negRate >= 0.4) return "BEARISH";
        return "NEUTRAL";
    }

    private void cacheUntilMidnight(String cacheKey, WatchlistSectorResDTO.SectorAnalysisList dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            long ttl = Math.max(ChronoUnit.SECONDS.between(LocalDateTime.now(),
                    LocalDate.now().plusDays(1).atStartOfDay()), 1L);
            redisUtil.save(cacheKey, json, ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("[SectorAnalysisService] 캐시 저장 실패: {}", e.getMessage());
        }
    }
}
