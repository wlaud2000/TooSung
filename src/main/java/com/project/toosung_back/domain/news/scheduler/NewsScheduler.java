package com.project.toosung_back.domain.news.scheduler;

import com.project.toosung_back.domain.news.service.NewsAnalysisService;
import com.project.toosung_back.domain.news.service.NewsCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsCollectorService newsCollectorService;
    private final NewsAnalysisService newsAnalysisService;

    // 30분 간격 실행 (서버 시작 후 1분 뒤 첫 실행)
    @Scheduled(initialDelay = 60_000, fixedRate = 30 * 60 * 1000)
    public void collectNews() {
        log.info("[NewsScheduler] 스케줄 실행 시작");
        newsCollectorService.collectAll();
        newsAnalysisService.analyzeUnanalyzedNews();
    }
}