package com.project.toosung_back.domain.news.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.news.converter.NewsConverter;
import com.project.toosung_back.domain.news.dto.response.NewsResDTO;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.exception.NewsErrorCode;
import com.project.toosung_back.domain.news.exception.NewsException;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import com.project.toosung_back.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private static final String CACHE_PREFIX = "news:list:";
    private static final long CACHE_TTL_SECONDS = 1800L;

    private final NewsRepository newsRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public NewsResDTO.NewsDetail getNewsDetail(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsException(NewsErrorCode.NEWS_NOT_FOUND));

        Optional<NewsAnalysis> analysis = newsAnalysisRepository.findByNewsId(newsId);

        return NewsConverter.toNewsDetail(news, analysis);
    }

    @Transactional(readOnly = true)
    public NewsResDTO.NewsList getNews(Long stockId, Long cursor, int size, Sentiment sentiment) {
        String cacheKey = CACHE_PREFIX + stockId + ":cursor:" + cursor + ":size:" + size + ":sentiment:" + sentiment;

        if (redisUtil.hasKey(cacheKey)) {
            String cached = redisUtil.get(cacheKey);
            try {
                return objectMapper.readValue(cached, NewsResDTO.NewsList.class);
            } catch (JsonProcessingException e) {
                log.warn("[NewsQueryService] 캐시 역직렬화 실패, 재조회 - key={}", cacheKey);
                redisUtil.delete(cacheKey);
            }
        }

        Slice<News> slice = (sentiment != null)
                ? newsRepository.findByStockIdAndSentimentWithCursor(stockId, sentiment, cursor, PageRequest.of(0, size))
                : newsRepository.findByStockIdWithCursor(stockId, cursor, PageRequest.of(0, size));

        List<Long> newsIds = slice.getContent().stream()
                .map(News::getId)
                .toList();

        Map<Long, NewsAnalysis> analysisMap = newsAnalysisRepository.findAllByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getNews().getId(), a -> a));

        List<NewsResDTO.NewsItem> items = slice.getContent().stream()
                .map(news -> NewsConverter.toNewsItem(news, analysisMap.get(news.getId())))
                .toList();

        Long nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getId()
                : null;

        NewsResDTO.NewsList result = NewsResDTO.NewsList.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();

        try {
            redisUtil.save(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("[NewsQueryService] 캐시 저장 실패 - key={}", cacheKey);
        }

        return result;
    }
}
