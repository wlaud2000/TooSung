package com.project.toosung_back.domain.news.converter;

import com.project.toosung_back.domain.news.dto.response.NewsResDTO;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsConverter {

    public static NewsResDTO.NewsDetail toNewsDetail(News news, Optional<NewsAnalysis> analysis) {
        return NewsResDTO.NewsDetail.builder()
                .newsId(news.getId())
                .title(news.getTitle())
                .url(news.getUrl())
                .thumbnailUrl(news.getThumbnailUrl())
                .source(news.getSource())
                .publishedAt(news.getPublishedAt())
                .summary(analysis.map(NewsAnalysis::getSummary).orElse(null))
                .sentiment(analysis.map(a -> a.getSentiment().name()).orElse(null))
                .sentimentReason(analysis.map(NewsAnalysis::getSentimentReason).orElse(null))
                .analyzedAt(analysis.map(NewsAnalysis::getAnalyzedAt).orElse(null))
                .build();
    }

    public static NewsResDTO.NewsItem toNewsItem(News news, NewsAnalysis analysis) {
        return NewsResDTO.NewsItem.builder()
                .newsId(news.getId())
                .title(news.getTitle())
                .summary(analysis != null ? analysis.getSummary() : null)
                .sentiment(analysis != null ? analysis.getSentiment().name() : null)
                .url(news.getUrl())
                .publishedAt(news.getPublishedAt())
                .source(news.getSource())
                .build();
    }
}
