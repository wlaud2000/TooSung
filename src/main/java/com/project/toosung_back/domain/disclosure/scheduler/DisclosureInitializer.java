package com.project.toosung_back.domain.disclosure.scheduler;

import com.project.toosung_back.domain.disclosure.entity.DisclosureSource;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.disclosure.service.DisclosureCollectorService;
import com.project.toosung_back.domain.disclosure.service.EdgarCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Order(4)
@Component
@RequiredArgsConstructor
public class DisclosureInitializer implements ApplicationRunner {

    private static final int DAYS_TO_BACKFILL = 90;

    private final DisclosureRepository disclosureRepository;
    private final DisclosureCollectorService disclosureCollectorService;
    private final EdgarCollectorService edgarCollectorService;

    @Override
    public void run(ApplicationArguments args) {
        runDartBackfill();
        runEdgarBackfill();
    }

    private void runDartBackfill() {
        long dartCount = disclosureRepository.countBySource(DisclosureSource.DART);
        // source가 null인 기존 DART 데이터 포함하여 판단
        if (dartCount > 0 || disclosureRepository.count() > 0) {
            log.info("[DisclosureInitializer] DART 공시 데이터 존재. 소급 수집 스킵");
            return;
        }

        log.info("[DisclosureInitializer] DART 소급 수집 시작: 최근 {}일", DAYS_TO_BACKFILL);
        LocalDate today = LocalDate.now();
        for (int i = DAYS_TO_BACKFILL; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;
            disclosureCollectorService.collectForDate(date);
        }
        log.info("[DisclosureInitializer] DART 소급 수집 완료");
    }

    private void runEdgarBackfill() {
        if (disclosureRepository.countBySource(DisclosureSource.EDGAR) > 0) {
            log.info("[DisclosureInitializer] EDGAR 공시 데이터 존재. 소급 수집 스킵");
            return;
        }

        log.info("[DisclosureInitializer] EDGAR 소급 수집 시작: 최근 {}일", DAYS_TO_BACKFILL);
        edgarCollectorService.collectBackfill(DAYS_TO_BACKFILL);
        log.info("[DisclosureInitializer] EDGAR 소급 수집 완료");
    }
}
