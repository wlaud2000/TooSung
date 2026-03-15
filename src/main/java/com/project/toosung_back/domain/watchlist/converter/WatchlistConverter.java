package com.project.toosung_back.domain.watchlist.converter;

import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WatchlistConverter {

    public static WatchlistResDTO.WatchlistItem toResWatchlistItem(Watchlist watchlist) {
        return WatchlistResDTO.WatchlistItem.builder()
                .stockId(watchlist.getStock().getId())
                .name(watchlist.getStock().getName())
                .code(watchlist.getStock().getSymbol())
                .market(watchlist.getStock().getMarket())
                .build();
    }
}
