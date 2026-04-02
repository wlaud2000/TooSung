package com.project.toosung_back.domain.news.converter;

import com.project.toosung_back.domain.news.dto.response.NewsResDTO;
import com.project.toosung_back.domain.news.entity.News;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsConverter {

    public static NewsResDTO.NewsDetail toNewsDetail(News news) {
        return NewsResDTO.NewsDetail.builder()
                .newsId(news.getId())
                .title(news.getTitle())
                .url(news.getUrl())
                .thumbnailUrl(news.getThumbnailUrl())
                .source(news.getSource())
                .publishedAt(news.getPublishedAt())
                .sentiment(news.getSentiment().name())
                .aiSummary(news.getAiSummary())
                .aiAnalysis(news.getAiAnalysis())
                .aiAnalyzedAt(news.getAiAnalyzedAt())
                .build();
    }

    public static NewsResDTO.NewsItem toNewsItem(News news) {
        return NewsResDTO.NewsItem.builder()
                .newsId(news.getId())
                .title(news.getTitle())
                .summary(news.getAiSummary())
                .url(news.getUrl())
                .publishedAt(news.getPublishedAt())
                .source(news.getSource())
                .build();
    }
}
