package com.project.toosung_back.domain.briefing.controller.docs;

import com.project.toosung_back.domain.briefing.dto.response.BriefingResDTO;
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
                                                    "newsIds": [42, 15]
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
                                                    "newsIds": []
                                                }
                                            }
                                            """)
                            }
                    )
            )
    })
    CustomResponse<BriefingResDTO.BriefingDetail> getTodayBriefing(@Parameter(hidden = true) AuthUser authUser);
}