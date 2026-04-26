package com.project.toosung_back.domain.watchlist.dto.request;

import jakarta.validation.constraints.NotNull;

public class WatchlistReqDTO {

    public record AddWatchlist(
            @NotNull(message = "종목 ID는 필수입니다.")
            Long stockId
    ) {}
}
