package com.project.toosung_back.domain.news.service;

import com.project.toosung_back.domain.news.client.NaverNewsClient;
import com.project.toosung_back.domain.news.dto.response.NaverNewsResponse;
import com.project.toosung_back.domain.news.entity.RealEstateNews;
import com.project.toosung_back.domain.news.repository.RealEstateNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final RealEstateNewsRepository realEstateNewsRepository;
    private final Executor newsCollectorExecutor;

    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private static final List<String> TARGET_REGIONS = List.of(
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
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(3);

        List<String> urlCandidates = items.stream()
                .map(item -> (item.originalLink() != null && !item.originalLink().isBlank())
                        ? item.originalLink() : item.link())
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();

        Set<String> existingUrls = new HashSet<>(realEstateNewsRepository.findExistingUrls(urlCandidates));

        List<Set<String>> tokenizedTitles = new ArrayList<>();

        List<RealEstateNews> newsToSave = new ArrayList<>();
        int skipDuplicate = 0, skipOld = 0, skipRegion = 0, skipSimilar = 0;

        for (NaverNewsResponse.NaverNewsItem item : items) {
            String url = (item.originalLink() != null && !item.originalLink().isBlank())
                    ? item.originalLink() : item.link();

            if (url == null || url.isBlank() || existingUrls.contains(url)) {
                skipDuplicate++;
                continue;
            }

            LocalDateTime pubDate = parseDate(item.pubDate());
            if (pubDate.isBefore(oneMonthAgo)) {
                skipOld++;
                continue;
            }

            String title = cleanHtml(item.title());

            if (!isTitleRelevantToRegion(title, region)) {
                skipRegion++;
                continue;
            }

            Set<String> titleTokens = tokenize(title);
            if (isSimilarToAny(titleTokens, tokenizedTitles)) {
                skipSimilar++;
                continue;
            }

            newsToSave.add(RealEstateNews.builder()
                    .externalId(url)
                    .source("naver")
                    .title(title)
                    .url(url)
                    .publishedAt(pubDate)
                    .region(region)
                    .build());
            tokenizedTitles.add(titleTokens);
        }

        int actualSaved = 0, skipRace = 0;
        for (RealEstateNews news : newsToSave) {
            try {
                realEstateNewsRepository.save(news);
                actualSaved++;
            } catch (DataIntegrityViolationException e) {
                skipRace++;
            }
        }

        log.info("[RealEstate] 지역: {} - 저장: {}건 | 중복URL: {}건, 오래된날짜: {}건, 지역무관: {}건, 유사제목: {}건, 동시충돌: {}건",
                region, actualSaved, skipDuplicate, skipOld, skipRegion, skipSimilar, skipRace);
    }

    private boolean isTitleRelevantToRegion(String title, String region) {
        if (title.contains(region)) return true;
        // "남양주왕숙신도시" → "남양주왕숙"
        String stripped = region.replaceAll("(신도시|아파트)$", "").replaceAll("구$", "");
        if (stripped.length() >= 2 && title.contains(stripped)) return true;
        // 도시명(앞 2~3자) 제거 후 지구명만으로 매칭
        // 예: "남양주왕숙" → "왕숙", "인천계양" → "계양", "군포대야미" → "대야미"
        for (int prefixLen = 2; prefixLen <= Math.min(3, stripped.length() - 2); prefixLen++) {
            String districtPart = stripped.substring(prefixLen);
            if (districtPart.length() >= 2 && title.contains(districtPart)) return true;
        }
        return false;
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
