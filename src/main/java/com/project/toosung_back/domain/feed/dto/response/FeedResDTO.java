package com.project.toosung_back.domain.feed.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class FeedResDTO {

    @Builder
    public record FeedItem(
            String type,
            Long itemId,
            String stockName,
            String title,
            LocalDateTime publishedAt
    ) {}

    @Builder
    public record FeedList(
            List<FeedItem> items,
            String nextCursor,
            boolean hasNext
    ) {}
}