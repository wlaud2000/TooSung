package com.project.toosung_back.domain.disclosure.service;

import com.project.toosung_back.domain.disclosure.client.EdgarApiClient;
import com.project.toosung_back.domain.disclosure.client.dto.EdgarSubmissionsResponse;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.entity.DisclosureSource;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import com.project.toosung_back.global.cache.CacheEvictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdgarCollectorService {

    private static final Set<String> TARGET_FORMS = Set.of("8-K", "10-K", "10-Q");
    private static final long RATE_LIMIT_DELAY_MS = 150; // SEC: 10 req/s 제한 대응

    private final WatchlistRepository watchlistRepository;
    private final DisclosureRepository disclosureRepository;
    private final EdgarApiClient edgarApiClient;
    private final CacheEvictService cacheEvictService;

    public void collectAll() {
        collectSince(LocalDate.now().minusDays(2));
    }

    public void collectBackfill(int days) {
        collectSince(LocalDate.now().minusDays(days));
    }

    public void collectBackfillForStocks(List<Stock> stocks, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        log.info("[EdgarCollectorService] 소급 수집 시작: {}개 종목, from={}", stocks.size(), from);

        int totalSaved = 0;
        for (Stock stock : stocks) {
            totalSaved += collectForStock(stock, from);
            rateLimitSleep();
        }

        log.info("[EdgarCollectorService] 소급 수집 완료: 총 {}건 저장", totalSaved);
        stocks.forEach(stock -> cacheEvictService.evictDisclosureCache(stock.getId()));
    }

    public void collectSince(LocalDate from) {
        List<Stock> usStocks = watchlistRepository.findAllDistinctStocks().stream()
                .filter(s -> "US".equals(s.getCountry()) && s.getEdgarCik() != null)
                .toList();

        if (usStocks.isEmpty()) {
            log.info("[EdgarCollectorService] edgar_cik 보유 US 종목 없음. 수집 스킵");
            return;
        }

        log.info("[EdgarCollectorService] 수집 시작: from={}, US종목={}개", from, usStocks.size());

        int totalSaved = 0;
        for (Stock stock : usStocks) {
            totalSaved += collectForStock(stock, from);
            rateLimitSleep();
        }

        log.info("[EdgarCollectorService] 수집 완료: 총 {}건 저장", totalSaved);
        usStocks.forEach(stock -> cacheEvictService.evictDisclosureCache(stock.getId()));
    }

    private int collectForStock(Stock stock, LocalDate from) {
        EdgarSubmissionsResponse response = edgarApiClient.fetchSubmissions(stock.getEdgarCik());
        if (response == null) {
            log.warn("[EdgarCollectorService] submissions 조회 실패: symbol={}", stock.getSymbol());
            return 0;
        }

        List<EdgarSubmissionsResponse.FilingItem> items = response.toFilingItems();
        int savedCount = 0;
        for (EdgarSubmissionsResponse.FilingItem filing : items) {
            if (!TARGET_FORMS.contains(filing.form())) continue;

            LocalDate filingDate = LocalDate.parse(filing.filingDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            if (filingDate.isBefore(from)) break;

            if (disclosureRepository.existsByDartId(filing.accessionNumber())) continue;

            String url = buildFilingUrl(stock.getEdgarCik(), filing.accessionNumber(), filing.primaryDocument());

            disclosureRepository.save(Disclosure.builder()
                    .dartId(filing.accessionNumber())
                    .disclosureType(filing.form())
                    .url(url)
                    .publishedAt(filingDate.atStartOfDay())
                    .source(DisclosureSource.EDGAR)
                    .stock(stock)
                    .build());

            log.info("[EdgarCollectorService] 저장 완료: accNo={}, form={}, stock={}",
                    filing.accessionNumber(), filing.form(), stock.getSymbol());
            savedCount++;
        }
        return savedCount;
    }

    private String buildFilingUrl(String cik, String accessionNumber, String primaryDocument) {
        String accNoNoDash = accessionNumber.replace("-", "");
        long cikLong = Long.parseLong(cik);
        if (primaryDocument != null && !primaryDocument.isBlank()) {
            return String.format("https://www.sec.gov/Archives/edgar/data/%d/%s/%s",
                    cikLong, accNoNoDash, primaryDocument);
        }
        return String.format("https://www.sec.gov/Archives/edgar/data/%d/%s/%s-index.htm",
                cikLong, accNoNoDash, accessionNumber);
    }

    private void rateLimitSleep() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
