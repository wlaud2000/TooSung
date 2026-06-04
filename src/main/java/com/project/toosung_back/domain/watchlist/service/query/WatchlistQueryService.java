package com.project.toosung_back.domain.watchlist.service.query;

import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.watchlist.converter.WatchlistConverter;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WatchlistQueryService {

    private final WatchlistRepository watchlistRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final DisclosureRepository disclosureRepository;

    @Transactional(readOnly = true)
    public WatchlistResDTO.ResWatchlistList getWatchlist(Long memberId) {
        List<WatchlistResDTO.WatchlistItem> items = watchlistRepository.findByMember_IdAndDeletedAtIsNullOrderByPositionAsc(memberId)
                .stream()
                .map(WatchlistConverter::toResWatchlistItem)
                .toList();

        return WatchlistResDTO.ResWatchlistList.builder()
                .watchlist(items)
                .build();
    }

    @Transactional(readOnly = true)
    public WatchlistResDTO.WatchlistStatusList getWatchlistStatus(Long memberId) {
        List<Watchlist> watchlists = watchlistRepository.findByMember_IdAndDeletedAtIsNullOrderByPositionAsc(memberId);

        if (watchlists.isEmpty()) {
            return WatchlistResDTO.WatchlistStatusList.builder().watchlist(List.of()).build();
        }

        List<Long> stockIds = watchlists.stream().map(w -> w.getStock().getId()).toList();
        LocalDateTime from = LocalDateTime.now().minusHours(12);

        // 종목별 감성 카운트: Map<stockId, Map<sentiment, count>>
        Map<Long, Map<Sentiment, Integer>> sentimentMap = new HashMap<>();
        for (Object[] row : newsAnalysisRepository.countSentimentByStockIds(stockIds, from)) {
            Long stockId = (Long) row[0];
            Sentiment sentiment = (Sentiment) row[1];
            int count = ((Number) row[2]).intValue();
            sentimentMap.computeIfAbsent(stockId, k -> new HashMap<>()).put(sentiment, count);
        }

        // 종목별 공시 카운트: Map<stockId, count>
        Map<Long, Integer> disclosureMap = new HashMap<>();
        for (Object[] row : disclosureRepository.countByStockIds(stockIds, from)) {
            Long stockId = (Long) row[0];
            disclosureMap.put(stockId, ((Number) row[1]).intValue());
        }

        List<WatchlistResDTO.WatchlistStatusItem> items = watchlists.stream()
                .map(w -> {
                    Long stockId = w.getStock().getId();
                    Map<Sentiment, Integer> sentiments = sentimentMap.getOrDefault(stockId, Map.of());
                    return WatchlistResDTO.WatchlistStatusItem.builder()
                            .watchlistId(w.getId())
                            .stockId(stockId)
                            .name(w.getStock().getName())
                            .code(w.getStock().getSymbol())
                            .market(w.getStock().getMarket())
                            .positiveCount(sentiments.getOrDefault(Sentiment.POSITIVE, 0))
                            .negativeCount(sentiments.getOrDefault(Sentiment.NEGATIVE, 0))
                            .neutralCount(sentiments.getOrDefault(Sentiment.NEUTRAL, 0))
                            .disclosureCount(disclosureMap.getOrDefault(stockId, 0))
                            .build();
                })
                .toList();

        return WatchlistResDTO.WatchlistStatusList.builder().watchlist(items).build();
    }
}
