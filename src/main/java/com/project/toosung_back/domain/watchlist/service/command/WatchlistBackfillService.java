package com.project.toosung_back.domain.watchlist.service.command;

import com.project.toosung_back.domain.disclosure.entity.DisclosureSource;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.disclosure.service.DisclosureCollectorService;
import com.project.toosung_back.domain.disclosure.service.EdgarCollectorService;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistBackfillService {

    private static final int DAYS_TO_BACKFILL = 100;

    private final StockRepository stockRepository;
    private final DisclosureRepository disclosureRepository;
    private final DisclosureCollectorService disclosureCollectorService;
    private final EdgarCollectorService edgarCollectorService;

    @Async("watchlistBackfillExecutor")
    public void triggerIfNeeded(Long stockId) {
        Stock stock = stockRepository.findById(stockId).orElse(null);
        if (stock == null) return;

        if ("KR".equals(stock.getCountry())) {
            if (disclosureRepository.existsByStock_Id(stockId)) {
                log.info("[WatchlistBackfill] DART 공시 이미 존재, 소급 스킵: symbol={}", stock.getSymbol());
                return;
            }
            log.info("[WatchlistBackfill] DART 소급 수집 시작: symbol={}", stock.getSymbol());
            LocalDate today = LocalDate.now();
            for (int i = DAYS_TO_BACKFILL; i >= 1; i--) {
                LocalDate date = today.minusDays(i);
                DayOfWeek dow = date.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;
                disclosureCollectorService.collectForDateAndStocks(date, List.of(stock));
            }
            log.info("[WatchlistBackfill] DART 소급 수집 완료: symbol={}", stock.getSymbol());

        } else if ("US".equals(stock.getCountry()) && stock.getEdgarCik() != null) {
            if (disclosureRepository.existsByStock_IdAndSource(stockId, DisclosureSource.EDGAR)) {
                log.info("[WatchlistBackfill] EDGAR 공시 이미 존재, 소급 스킵: symbol={}", stock.getSymbol());
                return;
            }
            log.info("[WatchlistBackfill] EDGAR 소급 수집 시작: symbol={}", stock.getSymbol());
            edgarCollectorService.collectBackfillForStocks(List.of(stock), DAYS_TO_BACKFILL);
            log.info("[WatchlistBackfill] EDGAR 소급 수집 완료: symbol={}", stock.getSymbol());
        }
    }
}
