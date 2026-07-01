package com.project.toosung_back.domain.news.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class RealEstateNewsResDTO {

    @Builder
    public record RealEstateNewsItem(
            Long newsId,
            String title,
            String url,
            LocalDateTime publishedAt,
            String region,
            String sentiment,
            String sentimentReason,
            String summary,
            String keyPoints
    ) {}

    @Builder
    public record RealEstateNewsList(
            String region,
            String sentiment,
            List<RealEstateNewsItem> items,
            LocalDateTime nextCursor,
            boolean hasNext
    ) {}
}
