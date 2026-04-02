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
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "News API", description = "뉴스 관련 API")
public interface NewsDocs {

    @Operation(
            summary = "뉴스 상세 조회",
            description = "뉴스 ID로 단건 뉴스를 조회합니다. AI 분석 필드는 처리 전 null일 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "뉴스 상세 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "뉴스 상세 조회 성공",
                                        "result": {
                                            "newsId": 42,
                                            "title": "삼성전자, 3분기 영업이익 10조 돌파",
                                            "url": "https://...",
                                            "thumbnailUrl": "https://...",
                                            "source": "네이버뉴스",
                                            "publishedAt": "2026-04-02T09:30:00",
                                            "sentiment": "POSITIVE",
                                            "aiSummary": "삼성전자가 3분기 영업이익 10조원을 달성했습니다.",
                                            "aiAnalysis": "반도체 업황 회복과 HBM 수요 증가가 주요 요인으로...",
                                            "aiAnalyzedAt": "2026-04-02T10:00:00"
                                        }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 뉴스",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "isSuccess": false,
                                        "code": "NEWS404_1",
                                        "message": "존재하지 않는 뉴스입니다"
                                    }
                                    """)
                    )
            )
    })
    CustomResponse<NewsResDTO.NewsDetail> getNewsDetail(
            @Parameter(description = "뉴스 ID", required = true, example = "42")
            @PathVariable Long newsId
    );

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