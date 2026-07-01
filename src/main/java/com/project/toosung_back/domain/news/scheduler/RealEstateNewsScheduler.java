package com.project.toosung_back.domain.news.scheduler;

import com.project.toosung_back.domain.news.service.RealEstateNewsAnalysisService;
import com.project.toosung_back.domain.news.service.RealEstateNewsCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealEstateNewsScheduler {

    private final RealEstateNewsCollectorService realEstateNewsCollectorService;
    private final RealEstateNewsAnalysisService realEstateNewsAnalysisService;

    @Scheduled(cron = "0 0 7 * * *")
    public void collectAndAnalyze() {
        log.info("[RealEstateScheduler] 스케줄 실행 시작");
        realEstateNewsCollectorService.collectAll();
        realEstateNewsAnalysisService.analyzeUnanalyzedRealEstateNews();
        log.info("[RealEstateScheduler] 스케줄 실행 완료");
    }
}
