package com.project.toosung_back.domain.stock.service.command;

import com.project.toosung_back.domain.disclosure.client.EdgarApiClient;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class EdgarCorpCodeInitializer implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final EdgarApiClient edgarApiClient;

    @Override
    public void run(ApplicationArguments args) {
        List<Stock> targets = stockRepository.findByCountryAndEdgarCikIsNull("US");
        if (targets.isEmpty()) {
            log.info("[EdgarCorpCodeInitializer] edgar_cik 초기화 대상 없음. 스킵");
            return;
        }

        log.info("[EdgarCorpCodeInitializer] edgar_cik 초기화 시작: {}개 US 종목", targets.size());

        Map<String, String> tickerToCik = edgarApiClient.fetchTickerToCikMap();
        if (tickerToCik.isEmpty()) {
            log.error("[EdgarCorpCodeInitializer] ticker-CIK 매핑 다운로드 실패 - edgar_cik 초기화 중단");
            return;
        }
        log.info("[EdgarCorpCodeInitializer] SEC ticker-CIK 매핑: {}개", tickerToCik.size());

        int updatedCount = 0;
        for (Stock stock : targets) {
            String cik = tickerToCik.get(stock.getSymbol().toUpperCase());
            if (cik != null) {
                stock.updateEdgarCik(cik);
                updatedCount++;
            } else {
                log.warn("[EdgarCorpCodeInitializer] edgar_cik 매핑 실패: symbol={}", stock.getSymbol());
            }
        }

        stockRepository.saveAll(targets);
        log.info("[EdgarCorpCodeInitializer] edgar_cik 초기화 완료: {}건 업데이트", updatedCount);
    }
}
