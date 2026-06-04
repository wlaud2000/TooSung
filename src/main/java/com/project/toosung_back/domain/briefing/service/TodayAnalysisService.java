package com.project.toosung_back.domain.briefing.service;

import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.disclosure.repository.DisclosureAnalysisRepository;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.NewsStockRepository;
import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
// 관심종목에 등록된 종목들의 오늘 뉴스 분석 +  공시분석을 모아서 브리핑용 데이터로 반환하는 서비스
public class TodayAnalysisService {

    private static final int NEWS_FETCH_LIMIT = 30;
    private static final int NEWS_PER_STOCK = 2;
    private static final int MAX_NEWS = 10;
    private static final int DISCLOSURE_LIMIT = 5;

    private final WatchlistRepository watchlistRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final NewsStockRepository newsStockRepository;
    private final DisclosureAnalysisRepository disclosureAnalysisRepository;

    public TodayAnalysisResult aggregate(Long memberId) {
        List<Watchlist> watchlists = watchlistRepository.findByMember_IdAndDeletedAtIsNullOrderByPositionAsc(memberId)
                .stream()
                .filter(w -> !w.isDeleted())
                .toList();

        if (watchlists.isEmpty()) {
            return new TodayAnalysisResult(List.of(), List.of());
        }

        List<Long> stockIds = watchlists.stream()
                .map(w -> w.getStock().getId())
                .toList();
        Set<Long> stockIdSet = new HashSet<>(stockIds);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        List<NewsAnalysis> rawNews = newsAnalysisRepository.findTodayByStockIds(
                stockIds, todayStart, PageRequest.of(0, NEWS_FETCH_LIMIT));

        List<Long> newsIds = rawNews.stream().map(na -> na.getNews().getId()).toList();
        Map<Long, Long> newsIdToStockId = buildNewsIdToStockId(newsIds);

        List<NewsAnalysis> filteredNews = deduplicateAndFilter(rawNews, newsIdToStockId, stockIdSet);

        List<DisclosureAnalysis> disclosures = disclosureAnalysisRepository.findTodayByStockIds(
                stockIds, todayStart, PageRequest.of(0, DISCLOSURE_LIMIT));

        log.info("[TodayAnalysisService] 집계 완료 - memberId={}, news={}건, disclosure={}건",
                memberId, filteredNews.size(), disclosures.size());

        return new TodayAnalysisResult(filteredNews, disclosures);

    }

    private Map<Long, Long> buildNewsIdToStockId(List<Long> newsIds) {
        if (newsIds.isEmpty()) return Map.of();

        return newsStockRepository.findAllByNewsIdIn(newsIds).stream()
                .collect(Collectors.toMap(
                        ns -> ns.getNews().getId(),
                        ns -> ns.getStock().getId(),
                        (a, b) -> a // 같은 newsId가 여러번 나오면 처음 만난 stockId만 남기고 나머지는 버림
                ));
    }

    private List<NewsAnalysis> deduplicateAndFilter(
            List<NewsAnalysis> rawNews,
            Map<Long, Long> newsIdToStockId,
            Set<Long> watchlistStockIds
    ) {
        Set<Long> seenNewsIds = new LinkedHashSet<>();
        List<NewsAnalysis> deduplicated = rawNews.stream()
                .filter(na -> seenNewsIds.add(na.getId()))
                .collect(Collectors.toList());

        deduplicated.sort(Comparator.comparingInt(na ->
                na.getSentiment() == Sentiment.NEUTRAL ? 1 : 0));

        Map<Long, Integer> stockCount = new HashMap<>();
        List<NewsAnalysis> result = new ArrayList<>();

        for (NewsAnalysis na : deduplicated) {
            Long stockId = newsIdToStockId.get(na.getNews().getId());
            if (stockId == null || !watchlistStockIds.contains(stockId)) continue;

            int count = stockCount.getOrDefault(stockId, 0);
            if (count < NEWS_PER_STOCK) {
                result.add(na);
                stockCount.put(stockId, count + 1);
            }
            if (result.size() >= MAX_NEWS) break;
        }
        return result;
    }
}
