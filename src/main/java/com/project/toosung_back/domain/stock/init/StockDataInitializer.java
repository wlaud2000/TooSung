package com.project.toosung_back.domain.stock.init;

import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDataInitializer implements ApplicationRunner {

    private final StockRepository stockRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (stockRepository.count() > 0) {
            return;
        }

        ClassPathResource resource = new ClassPathResource("data/stocks.csv");
        List<Stock> stocks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length < 4) continue;

                stocks.add(Stock.builder()
                        .symbol(fields[0].trim())
                        .name(fields[1].trim())
                        .market(fields[2].trim())
                        .country(fields[3].trim())
                        .build());
            }
        }

        stockRepository.saveAll(stocks);
        log.info("종목 초기 데이터 {}건 적재 완료", stocks.size());
    }
}
