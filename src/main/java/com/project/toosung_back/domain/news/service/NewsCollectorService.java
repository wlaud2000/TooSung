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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectorService {

    private final WatchlistRepository watchlistRepository;
    private final NaverNewsClient naverNewsClient;
    private final NewsRepository newsRepository;
    private final NewsStockRepository newsStockRepository;
    private final CacheEvictService cacheEvictService;
    private final Executor newsCollectorExecutor;

    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    public void collectAll() {
        List<Stock> stocks = watchlistRepository.findAllDistinctStocks();
        log.info("[NewsScheduler] 뉴스 수집 시작 - 대상 종목 수: {}", stocks.size());

        List<CompletableFuture<Void>> futures = stocks.stream()
                .map(stock -> CompletableFuture.runAsync(() -> {
                    try {
                        collectForStock(stock);
                        cacheEvictService.evictNewsCache(stock.getId());
                    } catch (Exception e) {
                        log.error("[NewsScheduler] 종목 처리 중 오류 - 종목: {} ({}), error: {}",
                                stock.getName(), stock.getSymbol(), e.getMessage());
                    }
                }, newsCollectorExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("[NewsScheduler] 뉴스 수집 완료");
    }

    @Transactional
    public void collectForStock(Stock stock) {
        List<NaverNewsResponse.NaverNewsItem> items = naverNewsClient.fetchNews(stock.getName());

        List<String> urlCandidates = items.stream()
                .map(item -> (item.originalLink() != null && !item.originalLink().isBlank())
                        ? item.originalLink() : item.link())
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();

        Set<String> existingUrls = new HashSet<>(newsRepository.findExistingUrls(urlCandidates));

        List<Set<String>> tokenizedTitles = newsRepository
                .findTitlesByStockIdSince(stock.getId(), LocalDateTime.now().minusHours(24))
                .stream()
                .map(this::tokenize)
                .collect(Collectors.toCollection(ArrayList::new));

        List<News> newsToSave = new ArrayList<>();
        int savedCount = 0;
        int skippedCount = 0;

        for (NaverNewsResponse.NaverNewsItem item : items) {
            String url = (item.originalLink() != null && !item.originalLink().isBlank())
                    ? item.originalLink() : item.link();

            if (url == null || url.isBlank() || existingUrls.contains(url)) {
                skippedCount++;
                continue;
            }

            String title = cleanHtml(item.title());
            Set<String> titleTokens = tokenize(title);

            if (isSimilarToAny(titleTokens, tokenizedTitles)) {
                skippedCount++;
                continue;
            }

            newsToSave.add(News.builder()
                    .externalId(url)
                    .source("naver")
                    .title(title)
                    .url(url)
                    .publishedAt(parseDate(item.pubDate()))
                    .build());
            tokenizedTitles.add(titleTokens);
            savedCount++;
        }

        if (!newsToSave.isEmpty()) {
            List<News> savedNews = newsRepository.saveAll(newsToSave);
            List<NewsStock> newsStocksToSave = savedNews.stream()
                    .map(news -> NewsStock.builder().news(news).stock(stock).build())
                    .toList();
            newsStockRepository.saveAll(newsStocksToSave);
        }

        log.info("[NewsScheduler] 종목: {} ({}) - 저장: {}건, 중복 스킵: {}건",
                stock.getName(), stock.getSymbol(), savedCount, skippedCount);
    }

    private boolean isSimilarToAny(Set<String> titleTokens, List<Set<String>> candidateTokens) {
        return candidateTokens.stream().anyMatch(c -> jaccardSimilarity(titleTokens, c) >= 0.4);
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        long intersectionSize = a.stream().filter(b::contains).count();
        long unionSize = (long) a.size() + b.size() - intersectionSize;
        return unionSize == 0 ? 0.0 : (double) intersectionSize / unionSize;
    }

    private Set<String> tokenize(String title) {
        return Arrays.stream(title.split("[\\s\\p{Punct}]+"))
                .filter(w -> w.length() >= 2)
                .collect(Collectors.toSet());
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
