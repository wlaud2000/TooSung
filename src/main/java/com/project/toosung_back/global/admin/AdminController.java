package com.project.toosung_back.global.admin;

import com.project.toosung_back.domain.disclosure.service.DisclosureAnalysisService;
import com.project.toosung_back.domain.disclosure.service.DisclosureCollectorService;
import com.project.toosung_back.domain.news.service.NewsAnalysisService;
import com.project.toosung_back.domain.news.service.NewsCollectorService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final NewsCollectorService newsCollectorService;
    private final NewsAnalysisService newsAnalysisService;
    private final DisclosureCollectorService disclosureCollectorService;
    private final DisclosureAnalysisService disclosureAnalysisService;

    @PostMapping("/collect/news")
    public CustomResponse<String> collectNews() {
        log.info("[Admin] 뉴스 수집 수동 트리거");
        newsCollectorService.collectAll();
        newsAnalysisService.analyzeUnanalyzedNews();
        return CustomResponse.onSuccess("뉴스 수집 및 분석 시작", null);
    }

    @PostMapping("/collect/disclosures")
    public CustomResponse<String> collectDisclosures() {
        log.info("[Admin] 공시 수집 수동 트리거");
        disclosureCollectorService.collectAll();
        disclosureAnalysisService.analyzeUnanalyzedDisclosures();
        return CustomResponse.onSuccess("공시 수집 및 분석 시작", null);
    }

    @PostMapping("/collect/all")
    public CustomResponse<String> collectAll() {
        log.info("[Admin] 전체 수집 수동 트리거");
        newsCollectorService.collectAll();
        newsAnalysisService.analyzeUnanalyzedNews();
        disclosureCollectorService.collectAll();
        disclosureAnalysisService.analyzeUnanalyzedDisclosures();
        return CustomResponse.onSuccess("전체 수집 및 분석 시작", null);
    }
}
