package com.project.toosung_back.domain.stock.controller.docs;

import com.project.toosung_back.domain.stock.dto.response.StockResDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Stock API", description = "종목 관련 API")
public interface StockDocs {

    @Operation(
            summary = "종목 검색",
            description = "종목명 또는 종목코드로 주식 종목을 검색합니다. 부분 일치를 지원하며, 결과가 없으면 빈 배열을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "200",
                                    "message": "종목 검색 성공",
                                    "result": {
                                        "stocks": [
                                            {
                                                "stockId": 1,
                                                "name": "삼성전자",
                                                "code": "005930",
                                                "market": "KOSPI"
                                            },
                                            {
                                                "stockId": 2,
                                                "name": "삼성SDI",
                                                "code": "006400",
                                                "market": "KOSPI"
                                            }
                                        ]
                                    }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 결과 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "200",
                                    "message": "종목 검색 성공",
                                    "result": {
                                        "stocks": []
                                    }
                                }
                                """)
                    )
            )
    })
    CustomResponse<StockResDTO.ResStockSearchList> searchStocks(
            @Parameter(description = "검색할 종목명 또는 종목코드", required = true, example = "삼성") String keyword
    );
}
