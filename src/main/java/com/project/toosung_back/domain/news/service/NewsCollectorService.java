package com.project.toosung_back.domain.news.service;

import com.project.toosung_back.domain.news.client.NaverNewsClient;
import com.project.toosung_back.domain.news.dto.response.NaverNewsResponse;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsStock;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import com.project.toosung_back.domain.news.repository.NewsStockRepository;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectorService {

    private final WatchlistRepository watchlistRepository;
    private final NaverNewsClient naverNewsClient;
    private final NewsRepository newsRepository;
    private final NewsStockRepository newsStockRepository;

    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    public void collectAll() {
        List<Stock> stocks = watchlistRepository.findAllDistinctStocks();
        log.info("[NewsScheduler] 뉴스 수집 시작 - 대상 종목 수: {}", stocks.size());

        for (Stock stock : stocks) {
            try {
                collectForStock(stock);
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

        for (NaverNewsResponse.NaverNewsItem item : items) {
            String url = (item.originalLink() != null && !item.originalLink().isBlank())
                    ? item.originalLink()
                    : item.link();

            // url도 없으면 스킵
            if (url == null || url.isBlank()) {
                skippedCount++;
                continue;
            }

            // 중복 URL 필터링
            if (newsRepository.existsByUrl(url)) {
                skippedCount++;
                continue;
            }

            News news = News.builder()
                    .externalId(url)
                    .source("naver")
                    .title(cleanHtml(item.title()))
                    .url(url)
                    .publishedAt(parseDate(item.pubDate()))
                    .sentiment(Sentiment.NEUTRAL)
                    .build();

            newsRepository.save(news);

            NewsStock newsStock = NewsStock.builder()
                    .news(news)
                    .stock(stock)
                    .build();

            newsStockRepository.save(newsStock);
            savedCount++;
        }

        log.info("[NewsScheduler] 종목: {} ({}) - 저장: {}건, 중복 스킵: {}건",
                stock.getName(), stock.getSymbol(), savedCount, skippedCount);
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