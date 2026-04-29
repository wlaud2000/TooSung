package com.project.toosung_back.domain.watchlist.service.command;

import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.member.exception.MemberErrorCode;
import com.project.toosung_back.domain.member.exception.MemberException;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.stock.repository.StockRepository;
import com.project.toosung_back.domain.watchlist.converter.WatchlistConverter;
import com.project.toosung_back.domain.watchlist.dto.request.WatchlistReqDTO;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import com.project.toosung_back.domain.watchlist.exception.WatchlistErrorCode;
import com.project.toosung_back.domain.watchlist.exception.WatchlistException;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import com.project.toosung_back.global.cache.CacheEvictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistCommandService {

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;
    private final MemberRepository memberRepository;
    private final CacheEvictService cacheEvictService;

    @Transactional
    public WatchlistResDTO.WatchlistItem addWatchlist(Long memberId, WatchlistReqDTO.AddWatchlist reqDTO) {
        if (watchlistRepository.existsByMember_IdAndStock_IdAndDeletedAtIsNull(memberId, reqDTO.stockId())) {
            throw new WatchlistException(WatchlistErrorCode.WATCHLIST_ALREADY_EXISTS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Stock stock = stockRepository.findById(reqDTO.stockId())
                .orElseThrow(() -> new WatchlistException(WatchlistErrorCode.STOCK_NOT_FOUND));

        int nextPosition = (int) watchlistRepository.countByMember_IdAndDeletedAtIsNull(memberId) + 1;

        Watchlist saved = watchlistRepository.save(WatchlistConverter.toWatchlist(member, stock, nextPosition));
        cacheEvictService.evictBriefingCache(memberId);
        cacheEvictService.evictSectorAnalysisCache(memberId);
        return WatchlistConverter.toResWatchlistItem(saved);
    }

    @Transactional
    public void deleteWatchlist(Long memberId, Long watchlistId) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new WatchlistException(WatchlistErrorCode.WATCHLIST_NOT_FOUND));

        if (!watchlist.getMember().getId().equals(memberId)) {
            throw new WatchlistException(WatchlistErrorCode.WATCHLIST_ACCESS_DENIED);
        }

        watchlist.delete();
        cacheEvictService.evictBriefingCache(memberId);
        cacheEvictService.evictSectorAnalysisCache(memberId);
    }
}
