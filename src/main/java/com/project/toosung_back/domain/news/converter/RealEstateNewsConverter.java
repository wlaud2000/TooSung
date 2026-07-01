package com.project.toosung_back.domain.news.converter;

import com.project.toosung_back.domain.news.dto.response.RealEstateNewsResDTO;
import com.project.toosung_back.domain.news.entity.RealEstateNews;
import com.project.toosung_back.domain.news.entity.RealEstateNewsAnalysis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RealEstateNewsConverter {

    public static RealEstateNewsResDTO.RealEstateNewsItem toRealEstateNewsItem(
            RealEstateNews news, RealEstateNewsAnalysis analysis) {
        return RealEstateNewsResDTO.RealEstateNewsItem.builder()
                .newsId(news.getId())
                .title(news.getTitle())
                .url(news.getUrl())
                .publishedAt(news.getPublishedAt())
                .region(news.getRegion())
                .sentiment(analysis != null ? analysis.getSentiment().name() : null)
                .sentimentReason(analysis != null ? analysis.getSentimentReason() : null)
                .summary(analysis != null ? analysis.getSummary() : null)
                .keyPoints(analysis != null ? analysis.getKeyPoints() : null)
                .build();
    }
}
