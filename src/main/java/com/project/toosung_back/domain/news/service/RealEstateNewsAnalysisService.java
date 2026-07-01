package com.project.toosung_back.domain.news.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.news.ai.RealEstateNewsAnalysisPrompt;
import com.project.toosung_back.domain.news.ai.dto.RealEstateNewsAnalysisResult;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import com.project.toosung_back.global.openai.client.OpenAiClient;
import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;
import com.project.toosung_back.global.openai.dto.OpenAiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealEstateNewsAnalysisService {

    private static final int BATCH_SIZE = 10;

    private final NewsRepository newsRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @Async("newsAnalysisExecutor")
    public void analyzeUnanalyzedRealEstateNews() {
        int totalSuccess = 0;
        int totalFail = 0;

        while (true) {
            List<News> newsList = newsRepository.findUnanalyzedRealEstateNews(PageRequest.of(0, BATCH_SIZE));
            if (newsList.isEmpty()) {
                break;
            }

            log.info("[RealEstateAnalysis] 분석 배치 시작 - {}건", newsList.size());

            for (News news : newsList) {
                try {
                    analyzeAndSave(news);
                    totalSuccess++;
                } catch (Exception e) {
                    log.error("[RealEstateAnalysis] 분석 실패 - newsId={}, error={}", news.getId(), e.getMessage());
                    totalFail++;
                }
            }
        }

        log.info("[RealEstate] 분석 완료 - 성공: {}건, 실패: {}건", totalSuccess, totalFail);
    }

    private void analyzeAndSave(News news) throws Exception {
        OpenAiChatRequest request = RealEstateNewsAnalysisPrompt.build(news.getTitle(), news.getRegion());
        OpenAiChatResponse response = openAiClient.chat(request);

        String rawJson = response.choices().get(0).message().content();
        RealEstateNewsAnalysisResult result = objectMapper.readValue(rawJson, RealEstateNewsAnalysisResult.class);

        boolean isRelevant = result.isRelevant();

        String summary = (isRelevant && result.summary() != null) ? String.join("\n", result.summary()) : "";
        String keyPoints = (isRelevant && result.keyPoints() != null && !result.keyPoints().isEmpty())
                ? String.join("\n", result.keyPoints())
                : null;
        Sentiment sentiment = (isRelevant && result.sentiment() != null && !result.sentiment().isBlank())
                ? Sentiment.valueOf(result.sentiment())
                : Sentiment.NEUTRAL;

        NewsAnalysis analysis = NewsAnalysis.builder()
                .news(news)
                .summary(summary)
                .keyPoints(keyPoints)
                .sentiment(sentiment)
                .sentimentReason(result.sentimentReason())
                .isRelevant(isRelevant)
                .analyzedAt(LocalDateTime.now())
                .build();

        newsAnalysisRepository.save(analysis);

        log.info("[RealEstateAnalysis] 분석 저장 완료 - newsId={}, region={}, sentiment={}",
                news.getId(), news.getRegion(), sentiment);
    }
}
