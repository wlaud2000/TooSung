package com.project.toosung_back.domain.news.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.news.converter.RealEstateNewsConverter;
import com.project.toosung_back.domain.news.dto.response.RealEstateNewsResDTO;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.enums.Sentiment;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealEstateNewsQueryService {

    private static final String CACHE_PREFIX = "realestate:news:";
    private static final long CACHE_TTL_SECONDS = 3600L;

    private final NewsRepository newsRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public RealEstateNewsResDTO.RealEstateNewsList getRealEstateNews(
            String region, Sentiment sentiment, Long cursor, int size) {

        String cacheKey = CACHE_PREFIX + region + ":" + sentiment + ":" + cursor + ":" + size;

        if (redisUtil.hasKey(cacheKey)) {
            String cached = redisUtil.get(cacheKey);
            try {
                return objectMapper.readValue(cached, RealEstateNewsResDTO.RealEstateNewsList.class);
            } catch (JsonProcessingException e) {
                log.warn("[RealEstateNewsQueryService] 캐시 역직렬화 실패, 재조회 - key={}", cacheKey);
                redisUtil.delete(cacheKey);
            }
        }

        Slice<News> slice = (sentiment != null)
                ? newsRepository.findRealEstateNewsByRegionAndSentimentWithCursor(region, sentiment, cursor, PageRequest.of(0, size))
                : newsRepository.findRealEstateNewsByRegionWithCursor(region, cursor, PageRequest.of(0, size));

        List<Long> newsIds = slice.getContent().stream()
                .map(News::getId)
                .toList();

        Map<Long, NewsAnalysis> analysisMap = newsAnalysisRepository.findAllByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getNews().getId(), a -> a));

        List<RealEstateNewsResDTO.RealEstateNewsItem> items = slice.getContent().stream()
                .map(news -> RealEstateNewsConverter.toRealEstateNewsItem(news, analysisMap.get(news.getId())))
                .toList();

        Long nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getId()
                : null;

        RealEstateNewsResDTO.RealEstateNewsList result = RealEstateNewsResDTO.RealEstateNewsList.builder()
                .region(region)
                .sentiment(sentiment != null ? sentiment.name() : null)
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();

        try {
            redisUtil.save(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("[RealEstateNewsQueryService] 캐시 저장 실패 - key={}", cacheKey);
        }

        return result;
    }
}
