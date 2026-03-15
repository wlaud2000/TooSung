package com.project.toosung_back.domain.watchlist.dto.response;

import lombok.Builder;

import java.util.List;

public class WatchlistResDTO {

    @Builder
    public record WatchlistItem(
            Long stockId,
            String name,
            String code,
            String market
    ) {}

    @Builder
    public record ResWatchlistList(
            List<WatchlistItem> watchlist
    ) {}
}
