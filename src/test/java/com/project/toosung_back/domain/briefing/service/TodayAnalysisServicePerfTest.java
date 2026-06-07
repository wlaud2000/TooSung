package com.project.toosung_back.domain.briefing.service;

import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.entity.NewsStock;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import com.project.toosung_back.domain.news.repository.NewsStockRepository;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class TodayAnalysisServicePerfTest {

    @Autowired private EntityManagerFactory emf;
    @Autowired private NewsRepository newsRepository;
    @Autowired private NewsStockRepository newsStockRepository;
    @Autowired private NewsAnalysisRepository newsAnalysisRepository;

    @MockitoBean private WatchlistRepository watchlistRepository;

    private static final Long MEMBER_ID = 1L;
    private static final List<Long> STOCK_IDS = List.of(1L, 2L, 3L, 4L, 5L);

    @BeforeEach
    void setUp() {
        // 종목 5개 × 10건 뉴스 + NewsStock + NewsAnalysis 생성
        for (Long stockId : STOCK_IDS) {
            Stock stock = Stock.builder().id(stockId).build();
            for (int i = 0; i < 10; i++) {
                String url = "https://test.com/" + stockId + "/" + UUID.randomUUID();
                News news = newsRepository.save(News.builder()
                        .externalId(url).source("test")
                        .title("테스트 뉴스 " + stockId + "-" + i)
                        .url(url)
                        .publishedAt(LocalDateTime.now())
                        .build());

                newsStockRepository.save(NewsStock.builder().news(news).stock(stock).build());

                newsAnalysisRepository.save(NewsAnalysis.builder()
                        .news(news)
                        .summary("테스트 요약")
                        .sentiment(Sentiment.POSITIVE)
                        .isRelevant(true)
                        .analyzedAt(LocalDateTime.now())
                        .build());
            }
        }

        // Watchlist 목업
        List<Watchlist> watchlists = IntStream.range(0, STOCK_IDS.size())
                .mapToObj(i -> Watchlist.builder()
                        .position(i + 1)
                        .stock(Stock.builder().id(STOCK_IDS.get(i)).build())
                        .build())
                .toList();
        given(watchlistRepository.findByMember_IdAndDeletedAtIsNullOrderByPositionAsc(eq(MEMBER_ID)))
                .willReturn(watchlists);
    }

    @AfterEach
    void tearDown() {
        newsAnalysisRepository.deleteAll();
        newsStockRepository.deleteAll();
        newsRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────
    // Before: 뉴스 조회 후 newsStock 재조회 (이중 쿼리)
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("[Before] 종목 5개 — 이중 쿼리 (뉴스 조회 + newsStock 재조회)")
    void before_이중_쿼리() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        StopWatch sw = new StopWatch();
        sw.start();

        // Query 1: 뉴스 조회 (구 findTodayByStockIds 재현)
        List<Object[]> rows = newsAnalysisRepository.findTodayByStockIdsWithStockId(
                STOCK_IDS, LocalDate.now().atStartOfDay(), PageRequest.of(0, 30));

        // Query 2: newsStock 재조회 (구 buildNewsIdToStockId 재현)
        List<Long> newsIds = rows.stream()
                .map(row -> ((NewsAnalysis) row[0]).getNews().getId())
                .toList();
        if (!newsIds.isEmpty()) {
            newsStockRepository.findAllByNewsIdIn(newsIds);
        }

        sw.stop();
        System.out.println();
        System.out.println("===== [Before] 이중 쿼리 — 종목 5개 × 10건 =====");
        System.out.printf("총 실행 시간 : %d ms%n", sw.getTotalTimeMillis());
        System.out.printf("총 쿼리 수   : %d 건%n", stats.getQueryExecutionCount());
        System.out.println("================================================");
    }

    // ─────────────────────────────────────────────────────────
    // After: 단일 쿼리로 뉴스 + stockId 동시 반환
    // ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("[After] 종목 5개 — 단일 쿼리 (stockId 스칼라 포함)")
    void after_단일_쿼리() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        StopWatch sw = new StopWatch();
        sw.start();

        // Query 1개: 뉴스 + stockId 동시 반환 → newsStock 재조회 불필요
        newsAnalysisRepository.findTodayByStockIdsWithStockId(
                STOCK_IDS, LocalDate.now().atStartOfDay(), PageRequest.of(0, 30));

        sw.stop();
        System.out.println();
        System.out.println("===== [After] 단일 쿼리 — 종목 5개 × 10건 =====");
        System.out.printf("총 실행 시간 : %d ms%n", sw.getTotalTimeMillis());
        System.out.printf("총 쿼리 수   : %d 건%n", stats.getQueryExecutionCount());
        System.out.println("===============================================");
    }
}
