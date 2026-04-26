package com.project.toosung_back.domain.news.ai;

import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;

import java.util.List;

public class NewsAnalysisPrompt {

    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 500;
    private static final double TEMPERATURE = 0.3;

    private static final String SYSTEM_PROMPT = """
            당신은 주식 투자 전문 애널리스트입니다.
            뉴스 제목을 분석하여 투자자에게 유용한 정보를 제공합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "isStockRelevant": true 또는 false,
              "summary": ["투자자 관점 핵심 첫 번째 문장", "두 번째 문장", "세 번째 문장"],
              "keyPoints": ["핵심 포인트 1", "핵심 포인트 2"],
              "sentiment": "POSITIVE 또는 NEGATIVE 또는 NEUTRAL",
              "sentimentReason": "감성 판단 근거 1문장"
            }

            규칙:
            - isStockRelevant: 아래 기준으로 판단
                true: 해당 기업이 기사의 주체이며, 실적·사업·제품·계약·규제·경영진 등 주가에 직접적 영향을 줄 수 있는 내용
                false: 다음 중 하나에 해당하는 경우
                  - 채용·인사시험·사내 행사·CSR 활동 등 주가와 무관한 내용
                  - 증권사·애널리스트·펀드 등 타 기관이 기사의 주체이며 해당 기업은 분석 대상으로만 언급된 경우 (예: "키움증권, OO 목표주가 상향")
                  - 타 기업 기사에서 해당 기업이 부수적으로 언급만 된 경우
            - isStockRelevant가 false이면 summary, keyPoints, sentiment, sentimentReason은 빈 값으로 반환
            - summary: 뉴스의 핵심을 투자자 관점에서 3개의 문장으로 요약
            - keyPoints: 투자자가 주목해야 할 핵심 포인트 2~3개
            - sentiment: 아래 기준으로 판단
                POSITIVE (호재): 실적 개선, 수주·계약 체결, 신제품 출시, 규제 완화, 배당 증가·자사주 매입 등 주가에 긍정적 영향이 예상될 때
                NEGATIVE (악재): 실적 악화, 소송·제재·벌금, 규제 강화, 경영진 이슈, 공급망 차질 등 주가에 부정적 영향이 예상될 때
                NEUTRAL: 단순 현황 보고, 업계 동향, 정보성 기사 등 주가 영향이 불분명하거나 양면적일 때
            - sentimentReason: 위 기준 중 어떤 항목에 해당하는지 포함해 1문장으로 설명
            """;

    private NewsAnalysisPrompt() {}

    public static OpenAiChatRequest build(String title, String stockName) {
        String userContent = "뉴스 제목: " + title + "\n관련 종목: " + stockName;

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
