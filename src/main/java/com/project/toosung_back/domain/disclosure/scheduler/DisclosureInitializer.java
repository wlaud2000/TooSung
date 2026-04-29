package com.project.toosung_back.domain.disclosure.scheduler;

import com.project.toosung_back.domain.disclosure.entity.DisclosureSource;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.disclosure.service.DisclosureCollectorService;
import com.project.toosung_back.domain.disclosure.service.EdgarCollectorService;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Order(4)
@Component
@RequiredArgsConstructor
public class DisclosureInitializer implements ApplicationRunner {

    private static final int DAYS_TO_BACKFILL = 100;

    private final DisclosureRepository disclosureRepository;
    private final DisclosureCollectorService disclosureCollectorService;
    private final EdgarCollectorService edgarCollectorService;
    private final WatchlistRepository watchlistRepository;

    @Override
    public void run(ApplicationArguments args) {
        runDartBackfill();
        runEdgarBackfill();
    }

    private void runDartBackfill() {
        List<Stock> krStocks = watchlistRepository.findAllDistinctStocks().stream()
                .filter(s -> "KR".equals(s.getCountry()))
                .toList();

        List<Stock> needsBackfill = krStocks.stream()
                .filter(s -> !disclosureRepository.existsByStock_Id(s.getId()))
                .toList();

        if (needsBackfill.isEmpty()) {
            log.info("[DisclosureInitializer] 모든 KR 종목 DART 공시 존재. 소급 수집 스킵");
            return;
        }

        log.info("[DisclosureInitializer] DART 소급 수집 시작: {}개 종목, 최근 {}일",
                needsBackfill.stream().map(Stock::getSymbol).toList(), DAYS_TO_BACKFILL);
        LocalDate today = LocalDate.now();
        for (int i = DAYS_TO_BACKFILL; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;
            disclosureCollectorService.collectForDateAndStocks(date, needsBackfill);
        }
        log.info("[DisclosureInitializer] DART 소급 수집 완료");
    }

    private void runEdgarBackfill() {
        List<Stock> usStocks = watchlistRepository.findAllDistinctStocks().stream()
                .filter(s -> "US".equals(s.getCountry()) && s.getEdgarCik() != null)
                .toList();

        List<Stock> needsBackfill = usStocks.stream()
                .filter(s -> !disclosureRepository.existsByStock_IdAndSource(s.getId(), DisclosureSource.EDGAR))
                .toList();

        if (needsBackfill.isEmpty()) {
            log.info("[DisclosureInitializer] 모든 US 종목 EDGAR 공시 존재. 소급 수집 스킵");
            return;
        }

        log.info("[DisclosureInitializer] EDGAR 소급 수집 시작: {}개 종목, 최근 {}일",
                needsBackfill.stream().map(Stock::getSymbol).toList(), DAYS_TO_BACKFILL);
        edgarCollectorService.collectBackfillForStocks(needsBackfill, DAYS_TO_BACKFILL);
        log.info("[DisclosureInitializer] EDGAR 소급 수집 완료");
    }
}
