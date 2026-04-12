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
              "summary": ["투자자 관점 핵심 첫 번째 문장", "두 번째 문장", "세 번째 문장"],
              "keyPoints": ["핵심 포인트 1", "핵심 포인트 2"],
              "sentiment": "POSITIVE 또는 NEGATIVE 또는 NEUTRAL",
              "sentimentReason": "감성 판단 근거 1문장"
            }

            규칙:
            - summary: 뉴스의 핵심을 투자자 관점에서 3개의 문장으로 요약
            - keyPoints: 투자자가 주목해야 할 핵심 포인트 2~3개
            - sentiment: 해당 뉴스가 주가에 미치는 영향 방향 (POSITIVE/NEGATIVE/NEUTRAL)
            - sentimentReason: 감성 판단의 근거를 1문장으로 설명
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
