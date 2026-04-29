package com.project.toosung_back.domain.watchlist.dto.response;

import lombok.Builder;

import java.util.List;

public class WatchlistResDTO {

    @Builder
    public record WatchlistItem(
            Long watchlistId,
            Long stockId,
            String name,
            String code,
            String market
    ) {}

    @Builder
    public record ResWatchlistList(
            List<WatchlistItem> watchlist
    ) {}

    @Builder
    public record WatchlistStatusItem(
            Long watchlistId,
            Long stockId,
            String name,
            String code,
            String market,
            int positiveCount,
            int negativeCount,
            int neutralCount,
            int disclosureCount
    ) {}

    @Builder
    public record WatchlistStatusList(
            List<WatchlistStatusItem> watchlist
    ) {}
}
