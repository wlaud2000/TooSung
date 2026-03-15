package com.project.toosung_back.domain.watchlist.controller;

import com.project.toosung_back.domain.watchlist.controller.docs.WatchlistDocs;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.domain.watchlist.service.query.WatchlistQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.annotation.CurrentUser;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/watchlist")
public class WatchlistController implements WatchlistDocs {

    private final WatchlistQueryService watchlistQueryService;

    @Override
    @GetMapping
    public CustomResponse<WatchlistResDTO.ResWatchlistList> getWatchlist(@CurrentUser AuthUser authUser) {
        WatchlistResDTO.ResWatchlistList resDTO = watchlistQueryService.getWatchlist(authUser.getMemberId());
        return CustomResponse.onSuccess("관심 종목 조회 성공", resDTO);
    }
}
