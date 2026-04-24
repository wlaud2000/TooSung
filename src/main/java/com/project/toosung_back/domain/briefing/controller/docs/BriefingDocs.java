package com.project.toosung_back.domain.briefing.controller.docs;

import com.project.toosung_back.domain.briefing.dto.response.BriefingResDTO;
import com.project.toosung_back.domain.briefing.dto.response.BriefingSourceResDTO;
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

@Tag(name = "Briefing API", description = "개인화 브리핑 관련 API")
public interface BriefingDocs {

    @Operation(
            summary = "오늘의 브리핑 조회",
            description = "관심 종목 기반 오늘의 맞춤 투자 브리핑을 조회합니다. 하루 1회 LLM으로 생성되며 Redis에 자정까지 캐싱됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "브리핑 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = {
                                    @ExampleObject(name = "정상 브리핑", value = """
                                            {
                                                "isSuccess": true,
                                                "code": "200",
                                                "message": "오늘의 브리핑 조회 성공",
                                                "result": {
                                                    "title": "HBM 수혜 기대감에 반도체 강세",
                                                    "summary": "지난번 관심 가졌던 HBM 관련 새 소식이에요. SK하이닉스가 오늘 대규모 HBM 공급 계약을 체결하면서 긍정적인 흐름을 보이고 있어요. 삼성전자도 3분기 영업이익이 시장 기대치를 웃돌면서 반도체 섹터 전반이 활기를 띠고 있어요.",
                                                    "newsIds": [42, 15],
                                                    "disclosureIds": [7, 3]
                                                }
                                            }
                                            """),
                                    @ExampleObject(name = "분석 항목 없음 (신규 유저 / 장외 시간)", value = """
                                            {
                                                "isSuccess": true,
                                                "code": "200",
                                                "message": "오늘의 브리핑 조회 성공",
                                                "result": {
                                                    "title": "오늘 브리핑을 준비 중입니다.",
                                                    "summary": "아직 분석된 항목이 없어요. 잠시 후 다시 확인해주세요.",
                                                    "newsIds": [],
                                                    "disclosureIds": []
                                                }
                                            }
                                            """)
                            }
                    )
            )
    })
    CustomResponse<BriefingResDTO.BriefingDetail> getTodayBriefing(@Parameter(hidden = true) AuthUser authUser);

    @Operation(
            summary = "브리핑 참고 뉴스/공시 목록 조회",
            description = "오늘의 브리핑 생성 시 참고한 뉴스와 공시 목록을 반환합니다. type 필드로 NEWS/DISCLOSURE 구분. 공시의 sentiment는 null입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "브리핑 참고 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = {
                                    @ExampleObject(name = "뉴스 + 공시 혼합", value = """
                                            {
                                                "isSuccess": true,
                                                "code": "200",
                                                "message": "브리핑 참고 목록 조회 성공",
                                                "result": {
                                                    "items": [
                                                        {
                                                            "id": 42,
                                                            "title": "SK하이닉스, 엔비디아와 HBM 공급 계약 체결",
                                                            "sentiment": "POSITIVE",
                                                            "url": "https://news.naver.com/...",
                                                            "type": "NEWS"
                                                        },
                                                        {
                                                            "id": 7,
                                                            "title": "사업보고서",
                                                            "sentiment": null,
                                                            "url": "https://dart.fss.or.kr/...",
                                                            "type": "DISCLOSURE"
                                                        }
                                                    ]
                                                }
                                            }
                                            """),
                                    @ExampleObject(name = "브리핑 없음 (빈 배열)", value = """
                                            {
                                                "isSuccess": true,
                                                "code": "200",
                                                "message": "브리핑 참고 목록 조회 성공",
                                                "result": {
                                                    "items": []
                                                }
                                            }
                                            """)
                            }
                    )
            )
    })
    CustomResponse<BriefingSourceResDTO.SourceList> getTodaySources(@Parameter(hidden = true) AuthUser authUser);
}