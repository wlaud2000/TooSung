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

    @Operation(
            summary = "관심 종목 삭제",
            description = "본인의 관심 종목을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "COMMON-200",
                                    "message": "관심 종목 삭제 성공",
                                    "data": null
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "WATCHLIST-002",
                                    "message": "해당 관심 종목에 접근할 권한이 없습니다."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 관심 종목",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "WATCHLIST-001",
                                    "message": "존재하지 않는 관심 종목입니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<Void> deleteWatchlist(
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(description = "삭제할 관심 종목 ID") Long watchlistId
    );
}
