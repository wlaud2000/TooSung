package com.project.toosung_back.domain.disclosure.scheduler;

import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.disclosure.service.DisclosureCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class DisclosureInitializer implements ApplicationRunner {

    private static final int DAYS_TO_BACKFILL = 90;

    private final DisclosureRepository disclosureRepository;
    private final DisclosureCollectorService disclosureCollectorService;

    @Override
    public void run(ApplicationArguments args) {
        if (disclosureRepository.count() > 0) {
            log.info("[DisclosureInitializer] 공시 데이터 존재. 소급 수집 스킵");
            return;
        }

        log.info("[DisclosureInitializer] 공시 데이터 없음. 최근 {}일 소급 수집 시작", DAYS_TO_BACKFILL);

        LocalDate today = LocalDate.now();
        for (int i = DAYS_TO_BACKFILL; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            disclosureCollectorService.collectForDate(date);
        }

        log.info("[DisclosureInitializer] 소급 수집 완료");
    }
}
