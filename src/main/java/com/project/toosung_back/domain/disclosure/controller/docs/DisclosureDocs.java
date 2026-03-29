package com.project.toosung_back.domain.disclosure.controller.docs;

import com.project.toosung_back.domain.disclosure.dto.response.DisclosureResDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Disclosure API", description = "공시 관련 API")
public interface DisclosureDocs {

    @Operation(
            summary = "공시 상세 조회",
            description = "공시 ID로 공시 상세 정보를 조회합니다. DS005 세부 데이터(rawData)와 AI 분석 결과가 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공시 상세 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "공시 상세 조회 성공",
                                        "result": {
                                            "id": 1,
                                            "disclosureType": "주요사항보고서(유상증자결정)",
                                            "url": "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=20260328000123",
                                            "publishedAt": "2026-03-28T00:00:00",
                                            "stockSymbol": "005930",
                                            "stockName": "삼성전자",
                                            "rawData": "{\\"status\\":\\"000\\",\\"list\\":[...]}",
                                            "aiSummary": null,
                                            "aiImpact": null,
                                            "aiInvestmentPoint": null,
                                            "aiAnalyzedAt": null
                                        }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공시를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "isSuccess": false,
                                        "code": "DISCLOSURE404_1",
                                        "message": "존재하지 않는 공시입니다",
                                        "result": null
                                    }
                                    """)
                    )
            )
    })
    CustomResponse<DisclosureResDTO.DisclosureDetail> getDisclosure(
            @Parameter(description = "조회할 공시 ID", required = true, example = "1") Long disclosureId
    );

    @Operation(
            summary = "관심 종목 공시 목록 조회",
            description = "특정 종목의 공시 목록을 커서 기반 페이지네이션으로 조회합니다. type 파라미터로 공시 유형을 필터링할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공시 목록 조회 성공 (다음 페이지 있음)",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(name = "hasNext=true", value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "공시 목록 조회 성공",
                                        "result": {
                                            "items": [
                                                {
                                                    "disclosureId": 42,
                                                    "title": "삼성전자 - 주요사항보고서(유상증자결정)",
                                                    "disclosureType": "주요사항보고서(유상증자결정)",
                                                    "submittedAt": "2026-03-28T00:00:00"
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
                    description = "공시 목록 조회 성공 (마지막 페이지)",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(name = "hasNext=false", value = """
                                    {
                                        "isSuccess": true,
                                        "code": "200",
                                        "message": "공시 목록 조회 성공",
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
    CustomResponse<DisclosureResDTO.DisclosureList> getDisclosures(
            @Parameter(description = "종목 ID", required = true, example = "1") Long stockId,
            @Parameter(description = "커서 (이전 응답의 nextCursor, 첫 페이지는 생략)", example = "42") Long cursor,
            @Parameter(description = "공시 유형 필터 키워드 (예: 유상증자, 합병, 사업보고서)", example = "유상증자") String type,
            @Parameter(description = "페이지 크기 (기본값: 20)", example = "20") int size
    );
}