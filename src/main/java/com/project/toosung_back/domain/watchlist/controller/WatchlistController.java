package com.project.toosung_back.domain.watchlist.controller;

import com.project.toosung_back.domain.watchlist.controller.docs.WatchlistDocs;
import com.project.toosung_back.domain.watchlist.dto.request.WatchlistReqDTO;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.domain.watchlist.service.command.WatchlistCommandService;
import com.project.toosung_back.domain.watchlist.service.query.WatchlistQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.annotation.CurrentUser;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/watchlist")
public class WatchlistController implements WatchlistDocs {

    private final WatchlistQueryService watchlistQueryService;
    private final WatchlistCommandService watchlistCommandService;

    @Override
    @PostMapping
    public CustomResponse<WatchlistResDTO.WatchlistItem> addWatchlist(
            @CurrentUser AuthUser authUser,
            @RequestBody @Valid WatchlistReqDTO.AddWatchlist reqDTO
    ) {
        WatchlistResDTO.WatchlistItem resDTO = watchlistCommandService.addWatchlist(authUser.getMemberId(), reqDTO);
        return CustomResponse.onSuccess("관심 종목 추가 성공", resDTO);
    }

    @Override
    @GetMapping
    public CustomResponse<WatchlistResDTO.ResWatchlistList> getWatchlist(@CurrentUser AuthUser authUser) {
        WatchlistResDTO.ResWatchlistList resDTO = watchlistQueryService.getWatchlist(authUser.getMemberId());
        return CustomResponse.onSuccess("관심 종목 조회 성공", resDTO);
    }

    @Override
    @DeleteMapping("/{watchlistId}")
    public CustomResponse<Void> deleteWatchlist(
            @CurrentUser AuthUser authUser,
            @PathVariable Long watchlistId
    ) {
        watchlistCommandService.deleteWatchlist(authUser.getMemberId(), watchlistId);
        return CustomResponse.onSuccess("관심 종목 삭제 성공", null);
    }
}
