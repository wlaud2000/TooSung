package com.project.toosung_back.domain.watchlist.controller;

import com.project.toosung_back.domain.watchlist.controller.docs.WatchlistDocs;
import com.project.toosung_back.domain.watchlist.dto.request.WatchlistReqDTO;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.domain.watchlist.dto.response.WatchlistSectorResDTO;
import com.project.toosung_back.domain.watchlist.service.command.WatchlistCommandService;
import com.project.toosung_back.domain.watchlist.service.query.SectorAnalysisService;
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
    private final SectorAnalysisService sectorAnalysisService;

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

    @GetMapping("/status")
    public CustomResponse<WatchlistResDTO.WatchlistStatusList> getWatchlistStatus(@CurrentUser AuthUser authUser) {
        WatchlistResDTO.WatchlistStatusList dto = watchlistQueryService.getWatchlistStatus(authUser.getMemberId());
        return CustomResponse.onSuccess("관심 종목 현황 조회 성공", dto);
    }

    @GetMapping("/sector-analysis")
    public CustomResponse<WatchlistSectorResDTO.SectorAnalysisList> getSectorAnalysis(@CurrentUser AuthUser authUser) {
        WatchlistSectorResDTO.SectorAnalysisList dto = sectorAnalysisService.getSectorAnalysis(authUser.getMemberId());
        return CustomResponse.onSuccess("섹터 분석 조회 성공", dto);
    }
}
