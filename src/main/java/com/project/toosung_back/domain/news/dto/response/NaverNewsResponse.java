package com.project.toosung_back.domain.news.dto.response;

import java.util.List;

public record NaverNewsResponse(
        int total,
        int start,
        int display,
        List<NaverNewsItem> items
) {
    public record NaverNewsItem(
            String title,
            String originalLink,
            String link,
            String description,
            String pubDate
    ) {
    }
}
