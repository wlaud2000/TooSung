package com.project.toosung_back.domain.news.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.news.ai.NewsAnalysisPrompt;
import com.project.toosung_back.domain.news.ai.dto.NewsAnalysisResult;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import com.project.toosung_back.domain.news.repository.NewsStockRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisService {

    private static final int BATCH_SIZE = 10;

    private final NewsRepository newsRepository;
    private final NewsStockRepository newsStockRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @Async("newsAnalysisExecutor")
    public void analyzeUnanalyzedNews() {
        List<News> newsList = newsRepository.findUnanalyzedNews(PageRequest.of(0, BATCH_SIZE));
        if (newsList.isEmpty()) {
            log.info("[NewsAnalysisService] 분석 대상 뉴스 없음");
            return;
        }

        log.info("[NewsAnalysisService] 분석 시작 - {}건", newsList.size());

        List<Long> newsIds = newsList.stream().map(News::getId).toList();

        Map<Long, String> newsIdToStockName = newsStockRepository.findAllByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(
                        ns -> ns.getNews().getId(),
                        ns -> ns.getStock().getName(),
                        (a, b) -> a
                ));

        int successCount = 0;
        int failCount = 0;

        for (News news : newsList) {
            String stockName = newsIdToStockName.getOrDefault(news.getId(), "알 수 없음");
            try {
                analyzeAndSave(news, stockName);
                successCount++;
            } catch (Exception e) {
                log.error("[NewsAnalysisService] 분석 실패 - newsId={}, error={}", news.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("[NewsAnalysisService] 분석 완료 - 성공: {}건, 실패: {}건", successCount, failCount);
    }

    private void analyzeAndSave(News news, String stockName) throws Exception {
        OpenAiChatRequest request = NewsAnalysisPrompt.build(news.getTitle(), stockName);
        OpenAiChatResponse response = openAiClient.chat(request);

        String rawJson = response.choices().get(0).message().content();
        NewsAnalysisResult result = objectMapper.readValue(rawJson, NewsAnalysisResult.class);

        String summary = String.join("\n", result.summary());
        String keyPoints = (result.keyPoints() != null && !result.keyPoints().isEmpty())
                ? String.join("\n", result.keyPoints())
                : null;
        Sentiment sentiment = Sentiment.valueOf(result.sentiment());

        NewsAnalysis analysis = NewsAnalysis.builder()
                .news(news)
                .summary(summary)
                .keyPoints(keyPoints)
                .sentiment(sentiment)
                .sentimentReason(result.sentimentReason())
                .analyzedAt(LocalDateTime.now())
                .build();

        newsAnalysisRepository.save(analysis);

        log.info("[NewsAnalysisService] 분석 저장 완료 - newsId={}, sentiment={}", news.getId(), sentiment);
    }
}
