package com.project.toosung_back.domain.watchlist.dto.response;

import lombok.Builder;

import java.util.List;

public class WatchlistSectorResDTO {

    @Builder
    public record SectorAnalysis(
            String sector,
            String sentiment,       // BULLISH / BEARISH / NEUTRAL
            String analysis,
            int positiveCount,
            int negativeCount,
            int neutralCount,
            List<String> stockNames
    ) {
        public int totalNews() {
            return positiveCount + negativeCount + neutralCount;
        }
    }

    @Builder
    public record SectorAnalysisList(
            List<SectorAnalysis> sectors
    ) {}
}
