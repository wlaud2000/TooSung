package com.project.toosung_back.domain.disclosure.scheduler;

import com.project.toosung_back.domain.disclosure.service.DisclosureAnalysisService;
import com.project.toosung_back.domain.disclosure.service.DisclosureCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisclosureScheduler {

    private final DisclosureCollectorService disclosureCollectorService;
    private final DisclosureAnalysisService disclosureAnalysisService;

    // 서버 시작 후 2분 뒤 첫 실행, 이후 1시간 간격
    @Scheduled(initialDelay = 120_000, fixedRate = 60 * 60 * 1000)
    public void collectDisclosures() {
        log.info("[DisclosureScheduler] 스케줄 실행 시작");
        disclosureCollectorService.collectAll();
        disclosureAnalysisService.analyzeUnanalyzedDisclosures();
    }
}
