package com.project.toosung_back.domain.news.controller.docs;

import com.project.toosung_back.domain.news.dto.response.NewsResDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "News API", description = "뉴스 관련 API")
public interface NewsDocs {

    @Operation(
            summary = "관심 종목 뉴스 목록 조회",
            description = "특정 종목의 뉴스 목록을 커서 기반 페이지네이션으로 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "뉴스 목록 조회 성공 (다음 페이지 있음)",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(name = "hasNext=true", value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "뉴스 목록 조회 성공",
                                        "result": {
                                            "items": [
                                                {
                                                    "newsId": 42,
                                                    "title": "삼성전자, 3분기 영업이익 10조 돌파",
                                                    "summary": null,
                                                    "url": "https://...",
                                                    "publishedAt": "2026-04-02T09:30:00",
                                                    "source": "네이버뉴스"
                                                }
                                            ],
                                            "nextCursor": 42,
                                            "hasNext": true
                                        }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "뉴스 목록 조회 성공 (마지막 페이지)",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(name = "hasNext=false", value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "뉴스 목록 조회 성공",
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
    CustomResponse<NewsResDTO.NewsList> getNews(
            @Parameter(description = "종목 ID", required = true, example = "1") Long stockId,
            @Parameter(description = "커서 (이전 응답의 nextCursor, 첫 페이지는 생략)", example = "42") Long cursor,
            @Parameter(description = "페이지 크기 (기본값: 20)", example = "20") int size
    );
}