package com.project.toosung_back.domain.news.ai;

import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;

import java.util.List;

public class RealEstateNewsAnalysisPrompt {

    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 500;
    private static final double TEMPERATURE = 0.3;

    private static final String SYSTEM_PROMPT = """
            당신은 부동산 투자 전문 애널리스트입니다.
            뉴스 제목과 지역명을 분석하여 해당 지역 부동산 시장에 대한 호재/악재를 판단합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "isRelevant": true 또는 false,
              "summary": ["핵심 첫 번째 문장", "두 번째 문장", "세 번째 문장"],
              "keyPoints": ["핵심 포인트 1", "핵심 포인트 2"],
              "sentiment": "POSITIVE 또는 NEGATIVE 또는 NEUTRAL",
              "sentimentReason": "감성 판단 근거 1문장"
            }

            감성 판단 기준:
            POSITIVE (호재):
              GTX·지하철 노선 확정, 재개발·재건축 허가, 용적률 완화,
              대형 기업/공공기관 이전, 개발 지구 지정, 교통망 개선 확정

            NEGATIVE (악재):
              금리 인상, LTV·DSR 대출 규제 강화, 공급 물량 급증,
              인구 감소, 개발 계획 취소·지연, 투기과열지구·조정대상지역 지정

            NEUTRAL:
              단순 시세 통계 발표, 방향성 불명확한 정책 예고,
              단순 거래량 현황 보도

            isRelevant 판단 기준:
              true: 해당 지역 부동산 시장에 직접적 영향을 줄 수 있는 기사
              false: 아래 중 하나에 해당하는 경우
                - 해당 지역과 무관한 기사
                - 광고성 분양 홍보
                - 타 지역 기사에서 해당 지역이 언급만 된 경우

            규칙:
            - isRelevant가 false이면 summary, keyPoints, sentiment, sentimentReason은 빈 값으로 반환
              (summary: [], keyPoints: [], sentiment: "NEUTRAL", sentimentReason: "")
            - summary: 투자자 관점에서 3개의 문장으로 요약
            - keyPoints: 투자자가 주목해야 할 핵심 포인트 2~3개
            - sentimentReason: 판단 근거를 1문장으로 설명
            """;

    private RealEstateNewsAnalysisPrompt() {}

    public static OpenAiChatRequest build(String title, String region) {
        String userContent = "뉴스 제목: " + title + "\n지역: " + region;

        return new OpenAiChatRequest(
                MODEL,
                List.of(
                        new OpenAiChatRequest.Message("system", SYSTEM_PROMPT),
                        new OpenAiChatRequest.Message("user", userContent)
                ),
                MAX_TOKENS,
                TEMPERATURE
        );
    }
}
