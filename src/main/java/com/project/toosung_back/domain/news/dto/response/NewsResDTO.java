package com.project.toosung_back.domain.news.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class NewsResDTO {

    @Builder
    public record NewsDetail(
            Long newsId,
            String title,
            String url,
            String thumbnailUrl,
            String source,
            LocalDateTime publishedAt,
            String summary,
            String sentiment,
            String sentimentReason,
            LocalDateTime analyzedAt
    ) {}

    @Builder
    public record NewsItem(
            Long newsId,
            String title,
            String summary,
            String sentiment,
            String sentimentReason,
            String url,
            LocalDateTime publishedAt,
            String source
    ) {}

    @Builder
    public record NewsList(
            List<NewsItem> items,
            Long nextCursor,
            boolean hasNext
    ) {}
}
