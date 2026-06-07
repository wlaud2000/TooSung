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
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class NewsCollectorServicePerfTest {

    @Autowired private NewsCollectorService newsCollectorService;
    @Autowired private EntityManagerFactory emf;
    @Autowired private NewsRepository newsRepository;
    @Autowired private NewsStockRepository newsStockRepository;

    @MockitoBean private NaverNewsClient naverNewsClient;
    @MockitoBean private WatchlistRepository watchlistRepository;
    @MockitoBean private CacheEvictService cacheEvictService;

    private List<NaverNewsResponse.NaverNewsItem> generateItems() {
        return IntStream.range(0, 100)
                .mapToObj(i -> new NaverNewsResponse.NaverNewsItem(
                        "뉴스 제목 " + UUID.randomUUID(),
                        "https://news.example.com/" + UUID.randomUUID(),
                        null,
                        "설명 " + i,
                        "Thu, 05 Jun 2026 10:00:00 +0900"
                ))
                .toList();
    }

    @BeforeEach
    void setUp() {
        List<Stock> stocks = IntStream.range(1, 6)
                .mapToObj(i -> Stock.builder()
                        .id((long) i)
                        .name("테스트종목" + i)
                        .symbol("TEST" + i)
                        .build())
                .toList();
        given(watchlistRepository.findAllDistinctStocks()).willReturn(stocks);
        given(naverNewsClient.fetchNews(any())).willAnswer(invocation -> generateItems());
    }

    @AfterEach
    void tearDown() {
        newsStockRepository.deleteAll();
        newsRepository.deleteAll();
    }

    // ───────────────────────────────────────────────
    // Before: N+1 방식 시뮬레이션 (종목 1개 × 100건)
    // ───────────────────────────────────────────────
    @Test
    @DisplayName("[Before] 종목 1개 × 100건 — N+1 방식")
    void before_N1_방식() {
        Stock stock = Stock.builder().id(1L).build();
        List<NaverNewsResponse.NaverNewsItem> items = generateItems();

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        StopWatch sw = new StopWatch();
        sw.start();

        List<String> recentTitles = new ArrayList<>();
        for (NaverNewsResponse.NaverNewsItem item : items) {
            String url = item.originalLink() != null ? item.originalLink() : item.link();
            if (url == null) continue;

            // N+1: 아이템마다 SELECT 1건
            if (newsRepository.existsByUrl(url)) continue;

            String title = item.title();
            if (legacyIsSimilar(title, recentTitles)) continue;

            // 건당 INSERT
            News news = newsRepository.save(
                    News.builder().externalId(url).source("naver")
                            .title(title).url(url).publishedAt(LocalDateTime.now()).build());
            newsStockRepository.save(NewsStock.builder().news(news).stock(stock).build());
            recentTitles.add(title);
        }

        sw.stop();
        System.out.println();
        System.out.println("===== [Before] N+1 방식 — 종목 1개 × 100건 =====");
        System.out.printf("총 실행 시간 : %d ms%n", sw.getTotalTimeMillis());
        System.out.printf("총 쿼리 수   : %d 건%n", stats.getQueryExecutionCount());
        System.out.printf("총 INSERT 수 : %d 건%n", stats.getEntityInsertCount());
        System.out.println("================================================");
    }

    // ───────────────────────────────────────────────
    // After: 개선된 방식 (종목 5개 × 100건, 병렬)
    // ───────────────────────────────────────────────
    @Test
    @DisplayName("[After] 종목 5개 × 100건 — 배치 IN절 + 병렬 처리")
    void after_배치_병렬_방식() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        StopWatch sw = new StopWatch();
        sw.start();

        newsCollectorService.collectAll();

        sw.stop();
        System.out.println();
        System.out.println("===== [After] 배치+병렬 방식 — 종목 5개 × 100건 =====");
        System.out.printf("총 실행 시간 : %d ms%n", sw.getTotalTimeMillis());
        System.out.printf("총 쿼리 수   : %d 건%n", stats.getQueryExecutionCount());
        System.out.printf("총 INSERT 수 : %d 건%n", stats.getEntityInsertCount());
        System.out.println("====================================================");
    }

    // ── 레거시 유사도 로직 재현 ──────────────────────
    private boolean legacyIsSimilar(String title, List<String> recentTitles) {
        Set<String> tokensA = legacyTokenize(title);
        return recentTitles.stream().anyMatch(t -> {
            Set<String> tokensB = legacyTokenize(t);        // 매번 재토큰화
            Set<String> intersection = new HashSet<>(tokensA);
            intersection.retainAll(tokensB);                // Set 복사본 생성
            Set<String> union = new HashSet<>(tokensA);
            union.addAll(tokensB);                          // Set 복사본 생성
            return !union.isEmpty() && (double) intersection.size() / union.size() >= 0.4;
        });
    }

    private Set<String> legacyTokenize(String title) {
        return Arrays.stream(title.split("[\\s\\p{Punct}]+"))
                .filter(w -> w.length() >= 2)
                .collect(Collectors.toSet());
    }
}
