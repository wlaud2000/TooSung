package com.project.toosung_back.domain.news.service;

import com.project.toosung_back.domain.news.client.NaverNewsClient;
import com.project.toosung_back.domain.news.dto.response.NaverNewsResponse;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsStock;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import com.project.toosung_back.domain.news.repository.NewsStockRepository;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import com.project.toosung_back.global.cache.CacheEvictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectorService {

    private final WatchlistRepository watchlistRepository;
    private final NaverNewsClient naverNewsClient;
    private final NewsRepository newsRepository;
    private final NewsStockRepository newsStockRepository;
    private final CacheEvictService cacheEvictService;

    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    public void collectAll() {
        List<Stock> stocks = watchlistRepository.findAllDistinctStocks();
        log.info("[NewsScheduler] 뉴스 수집 시작 - 대상 종목 수: {}", stocks.size());

        for (Stock stock : stocks) {
            try {
                collectForStock(stock);
                cacheEvictService.evictNewsCache(stock.getId());
            } catch (Exception e) {
                log.error("[NewsScheduler] 종목 처리 중 오류 - 종목: {} ({}), error: {}",
                        stock.getName(), stock.getSymbol(), e.getMessage());
            }
        }

        log.info("[NewsScheduler] 뉴스 수집 완료");
    }

    @Transactional
    public void collectForStock(Stock stock) {
        String query = stock.getName();
        List<NaverNewsResponse.NaverNewsItem> items = naverNewsClient.fetchNews(query);

        int savedCount = 0;
        int skippedCount = 0;

        List<String> recentTitles = newsRepository.findTitlesByStockIdSince(
                stock.getId(), LocalDateTime.now().minusHours(24));

        for (NaverNewsResponse.NaverNewsItem item : items) {
            String url = (item.originalLink() != null && !item.originalLink().isBlank())
                    ? item.originalLink()
                    : item.link();

            if (url == null || url.isBlank()) {
                skippedCount++;
                continue;
            }

            if (newsRepository.existsByUrl(url)) {
                skippedCount++;
                continue;
            }

            String title = cleanHtml(item.title());

            if (isSimilarToAny(title, recentTitles)) {
                skippedCount++;
                continue;
            }

            News news = News.builder()
                    .externalId(url)
                    .source("naver")
                    .title(title)
                    .url(url)
                    .publishedAt(parseDate(item.pubDate()))
                    .build();

            newsRepository.save(news);

            NewsStock newsStock = NewsStock.builder()
                    .news(news)
                    .stock(stock)
                    .build();

            newsStockRepository.save(newsStock);
            recentTitles.add(title);
            savedCount++;
        }

        log.info("[NewsScheduler] 종목: {} ({}) - 저장: {}건, 중복 스킵: {}건",
                stock.getName(), stock.getSymbol(), savedCount, skippedCount);
    }

    private boolean isSimilarToAny(String title, List<String> candidates) {
        return candidates.stream().anyMatch(c -> jaccardSimilarity(title, c) >= 0.6);
    }

    private double jaccardSimilarity(String a, String b) {
        Set<String> wordsA = tokenize(a);
        Set<String> wordsB = tokenize(b);

        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);

        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String title) {
        return Arrays.stream(title.split("[\\s\\p{Punct}]+"))
                .filter(w -> w.length() >= 2)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String cleanHtml(String html) {
        return Jsoup.parse(html).text();
    }

    private LocalDateTime parseDate(String pubDate) {
        try {
            return ZonedDateTime.parse(pubDate, NAVER_DATE_FORMAT).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
