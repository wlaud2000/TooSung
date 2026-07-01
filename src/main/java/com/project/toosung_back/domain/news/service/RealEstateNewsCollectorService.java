package com.project.toosung_back.domain.news.service;

import com.project.toosung_back.domain.news.client.NaverNewsClient;
import com.project.toosung_back.domain.news.dto.response.NaverNewsResponse;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.enums.NewsCategory;
import com.project.toosung_back.domain.news.repository.NewsRepository;
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
public class RealEstateNewsCollectorService {

    private final NaverNewsClient naverNewsClient;
    private final NewsRepository newsRepository;
    private final Executor newsCollectorExecutor;

    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private static final List<String> TARGET_REGIONS = List.of(
            // ── 서울 25개 구 ──
            "강남구", "강동구", "강북구", "강서구", "관악구",
            "광진구", "구로구", "금천구", "노원구", "도봉구",
            "동대문구", "동작구", "마포구", "서대문구", "서초구",
            "성동구", "성북구", "송파구", "양천구", "영등포구",
            "용산구", "은평구", "종로구", "중구", "중랑구",

            // ── 서울 핵심 동 ──
            "압구정동", "대치동", "반포동", "잠실동",
            "성수동", "한남동", "이촌동", "목동",
            "은마아파트", "둔촌주공",

            // ── 1기 신도시 ──
            "분당신도시", "일산신도시", "평촌신도시", "산본신도시", "중동신도시",

            // ── 2기 신도시 ──
            "판교신도시", "광교신도시", "동탄신도시",
            "김포한강신도시", "파주운정신도시",
            "양주옥정신도시", "양주회천신도시",
            "위례신도시", "미사강변도시", "다산신도시",
            "검단신도시", "오산세교신도시",

            // ── 3기 신도시 ──
            "남양주왕숙신도시", "하남교산신도시", "고양창릉신도시",
            "인천계양신도시", "부천대장신도시", "광명시흥신도시",
            "안산장상신도시", "의왕청계신도시", "군포대야미신도시"
    );

    public void collectAll() {
        log.info("[RealEstateScheduler] 뉴스 수집 시작 - 대상 지역 수: {}", TARGET_REGIONS.size());
        List<List<String>> batches = partition(TARGET_REGIONS, 20);

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            List<CompletableFuture<Void>> futures = batch.stream()
                    .map(region -> CompletableFuture.runAsync(
                            () -> {
                                try {
                                    collectForRegion(region);
                                } catch (Exception e) {
                                    log.error("[RealEstate] 지역 처리 중 오류 - 지역: {}, error: {}", region, e.getMessage());
                                }
                            }, newsCollectorExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            if (i < batches.size() - 1) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("[RealEstateScheduler] 뉴스 수집 완료");
    }

    @Transactional
    public void collectForRegion(String region) {
        List<NaverNewsResponse.NaverNewsItem> items = naverNewsClient.fetchNews(region + " 부동산");

        List<String> urlCandidates = items.stream()
                .map(item -> (item.originalLink() != null && !item.originalLink().isBlank())
                        ? item.originalLink() : item.link())
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();

        Set<String> existingUrls = new HashSet<>(newsRepository.findExistingUrls(urlCandidates));

        List<Set<String>> tokenizedTitles = new ArrayList<>();

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
                    .newsCategory(NewsCategory.REALESTATE)
                    .region(region)
                    .build());
            tokenizedTitles.add(titleTokens);
            savedCount++;
        }

        if (!newsToSave.isEmpty()) {
            newsRepository.saveAll(newsToSave);
        }

        log.info("[RealEstate] 지역: {} - 저장: {}건, 중복 스킵: {}건", region, savedCount, skippedCount);
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

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
