package com.project.toosung_back.domain.watchlist.service.command;

import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import com.project.toosung_back.domain.watchlist.exception.WatchlistErrorCode;
import com.project.toosung_back.domain.watchlist.exception.WatchlistException;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistCommandService {

    private final WatchlistRepository watchlistRepository;

    @Transactional
    public void deleteWatchlist(Long memberId, Long watchlistId) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new WatchlistException(WatchlistErrorCode.WATCHLIST_NOT_FOUND));

        if (!watchlist.getMember().getId().equals(memberId)) {
            throw new WatchlistException(WatchlistErrorCode.WATCHLIST_ACCESS_DENIED);
        }

        watchlist.delete();
    }
}
