package com.project.toosung_back.domain.news.service.query;

import com.project.toosung_back.domain.news.converter.RealEstateNewsConverter;
import com.project.toosung_back.domain.news.dto.response.RealEstateNewsResDTO;
import com.project.toosung_back.domain.news.entity.RealEstateNews;
import com.project.toosung_back.domain.news.entity.RealEstateNewsAnalysis;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.repository.RealEstateNewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.RealEstateNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RealEstateNewsQueryService {

    private final RealEstateNewsRepository realEstateNewsRepository;
    private final RealEstateNewsAnalysisRepository realEstateNewsAnalysisRepository;

    @Transactional(readOnly = true)
    public RealEstateNewsResDTO.RealEstateNewsList getRealEstateNews(
            String region, Sentiment sentiment, LocalDateTime cursor, int size) {

        Slice<RealEstateNews> slice = (sentiment != null)
                ? realEstateNewsRepository.findByRegionAndSentimentWithCursor(region, sentiment, cursor, PageRequest.of(0, size))
                : realEstateNewsRepository.findByRegionWithCursor(region, cursor, PageRequest.of(0, size));

        List<Long> newsIds = slice.getContent().stream()
                .map(RealEstateNews::getId)
                .toList();

        Map<Long, RealEstateNewsAnalysis> analysisMap = realEstateNewsAnalysisRepository
                .findAllByRealEstateNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getRealEstateNews().getId(), a -> a));

        List<RealEstateNewsResDTO.RealEstateNewsItem> items = slice.getContent().stream()
                .map(news -> RealEstateNewsConverter.toRealEstateNewsItem(news, analysisMap.get(news.getId())))
                .toList();

        LocalDateTime nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getPublishedAt()
                : null;

        return RealEstateNewsResDTO.RealEstateNewsList.builder()
                .region(region)
                .sentiment(sentiment != null ? sentiment.name() : null)
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();
    }
}
