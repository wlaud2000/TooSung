package com.project.toosung_back.domain.feed.controller.docs;

import com.project.toosung_back.domain.feed.dto.response.FeedResDTO;
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

@Tag(name = "Feed API", description = "통합 피드 관련 API")
public interface FeedDocs {

    @Operation(
            summary = "통합 피드 조회",
            description = "로그인 사용자의 관심 종목에 해당하는 뉴스와 공시를 최신순으로 통합 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "통합 피드 조회 성공 (다음 페이지 있음)",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(name = "hasNext=true", value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "통합 피드 조회 성공",
                                        "result": {
                                            "items": [
                                                {
                                                    "type": "NEWS",
                                                    "itemId": 42,
                                                    "stockName": "삼성전자",
                                                    "title": "삼성전자, 3분기 영업이익 10조 돌파",
                                                    "publishedAt": "2026-04-02T09:30:00"
                                                },
                                                {
                                                    "type": "DISCLOSURE",
                                                    "itemId": 7,
                                                    "stockName": "SK하이닉스",
                                                    "title": "유상증자결정",
                                                    "publishedAt": "2026-04-02T08:15:00"
                                                }
                                            ],
                                            "nextCursor": "2026-04-02T08:15:00",
                                            "hasNext": true
                                        }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "통합 피드 조회 성공 (마지막 페이지)",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(name = "hasNext=false", value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "통합 피드 조회 성공",
                                        "result": {
                                            "items": [],
                                            "nextCursor": null,
                                            "hasNext": false
                                        }
                                    }
                                    """)
                    )
            )
    })
    CustomResponse<FeedResDTO.FeedList> getFeed(
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(description = "커서 (이전 응답의 nextCursor, 첫 페이지는 생략)", example = "2026-04-02T09:30:00") String cursor,
            @Parameter(description = "페이지 크기 (기본값: 20)", example = "20") int size
    );
}