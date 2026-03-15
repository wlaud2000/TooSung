package com.project.toosung_back.domain.watchlist.service.query;

import com.project.toosung_back.domain.watchlist.converter.WatchlistConverter;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistQueryService {

    private final WatchlistRepository watchlistRepository;

    @Transactional(readOnly = true)
    public WatchlistResDTO.ResWatchlistList getWatchlist(Long memberId) {
        List<WatchlistResDTO.WatchlistItem> items = watchlistRepository.findByMember_IdOrderByPositionAsc(memberId)
                .stream()
                .map(WatchlistConverter::toResWatchlistItem)
                .toList();

        return WatchlistResDTO.ResWatchlistList.builder()
                .watchlist(items)
                .build();
    }
}
