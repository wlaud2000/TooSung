package com.project.toosung_back.domain.watchlist.controller.docs;

import com.project.toosung_back.domain.watchlist.dto.response.WatchlistResDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Watchlist API", description = "관심 종목 관련 API")
public interface WatchlistDocs {

    @Operation(
            summary = "관심 종목 목록 조회",
            description = "로그인한 사용자의 관심 종목 목록을 등록 순서대로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "COMMON-200",
                                    "message": "관심 종목 조회 성공",
                                    "data": {
                                        "watchlist": [
                                            {
                                                "stockId": 1,
                                                "name": "삼성전자",
                                                "code": "005930",
                                                "market": "KOSPI"
                                            },
                                            {
                                                "stockId": 2,
                                                "name": "Apple Inc.",
                                                "code": "AAPL",
                                                "market": "NASDAQ"
                                            }
                                        ]
                                    }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "SEC-001",
                                    "message": "인증이 필요합니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<WatchlistResDTO.ResWatchlistList> getWatchlist(@Parameter(hidden = true) AuthUser authUser);
}
