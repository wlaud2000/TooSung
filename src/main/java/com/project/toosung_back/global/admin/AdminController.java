package com.project.toosung_back.global.admin;

import com.project.toosung_back.domain.disclosure.service.DisclosureAnalysisService;
import com.project.toosung_back.domain.disclosure.service.DisclosureCollectorService;
import com.project.toosung_back.domain.disclosure.service.EdgarCollectorService;
import com.project.toosung_back.domain.news.service.NewsAnalysisService;
import com.project.toosung_back.domain.news.service.NewsCollectorService;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import com.project.toosung_back.domain.watchlist.service.command.WatchlistBackfillService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.cache.CacheEvictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final NewsCollectorService newsCollectorService;
    private final NewsAnalysisService newsAnalysisService;
    private final DisclosureCollectorService disclosureCollectorService;
    private final DisclosureAnalysisService disclosureAnalysisService;
    private final EdgarCollectorService edgarCollectorService;
    private final WatchlistBackfillService watchlistBackfillService;
    private final WatchlistRepository watchlistRepository;
    private final CacheEvictService cacheEvictService;

    @PostMapping("/collect/news")
    public CustomResponse<String> collectNews() {
        log.info("[Admin] 뉴스 수집 수동 트리거");
        newsCollectorService.collectAll();
        newsAnalysisService.analyzeUnanalyzedNews();
        return CustomResponse.onSuccess("뉴스 수집 및 분석 시작", null);
    }

    @PostMapping("/collect/disclosures")
    public CustomResponse<String> collectDisclosures() {
        log.info("[Admin] 공시 수집 수동 트리거");
        disclosureCollectorService.collectAll();
        edgarCollectorService.collectAll();
        disclosureAnalysisService.analyzeUnanalyzedDisclosures();
        return CustomResponse.onSuccess("공시 수집 및 분석 시작", null);
    }

    @PostMapping("/collect/all")
    public CustomResponse<String> collectAll() {
        log.info("[Admin] 전체 수집 수동 트리거");
        newsCollectorService.collectAll();
        newsAnalysisService.analyzeUnanalyzedNews();
        disclosureCollectorService.collectAll();
        edgarCollectorService.collectAll();
        disclosureAnalysisService.analyzeUnanalyzedDisclosures();
        return CustomResponse.onSuccess("전체 수집 및 분석 시작", null);
    }

    @PostMapping("/backfill/disclosures")
    public CustomResponse<String> backfillDisclosures() {
        log.info("[Admin] 공시 소급 수집 수동 트리거");
        List<Stock> stocks = watchlistRepository.findAllDistinctStocks();
        stocks.forEach(stock -> watchlistBackfillService.triggerIfNeeded(stock.getId()));
        return CustomResponse.onSuccess("공시 소급 수집 시작 (백그라운드)", null);
    }

    @DeleteMapping("/briefing/cache")
    public CustomResponse<String> clearBriefingCache() {
        log.info("[Admin] 브리핑 캐시 초기화");
        cacheEvictService.evictAllBriefingCaches();
        return CustomResponse.onSuccess("브리핑 캐시 초기화 완료", null);
    }

    @DeleteMapping("/sector-analysis/cache")
    public CustomResponse<String> clearSectorAnalysisCache() {
        log.info("[Admin] 섹터 분석 캐시 초기화");
        cacheEvictService.evictAllSectorAnalysisCaches();
        return CustomResponse.onSuccess("섹터 분석 캐시 초기화 완료", null);
    }
}
