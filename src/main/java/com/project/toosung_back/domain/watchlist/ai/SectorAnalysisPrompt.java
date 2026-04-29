package com.project.toosung_back.domain.watchlist.ai;

import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;

import java.util.List;
import java.util.Map;

public class SectorAnalysisPrompt {

    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 800;
    private static final double TEMPERATURE = 0.4;

    private static final String SYSTEM_PROMPT = """
            당신은 주식 시장 분석 전문가입니다.
            각 섹터별 오늘 뉴스를 바탕으로 섹터 분위기를 분석합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "섹터명1": "이 섹터의 오늘 분위기를 2문장으로 설명",
              "섹터명2": "이 섹터의 오늘 분위기를 2문장으로 설명"
            }

            규칙:
            - 각 섹터 분석은 오늘 뉴스 내용을 근거로 작성
            - 구체적인 이슈나 이유를 언급
            - 친근한 말투 사용 ("~이에요", "~보여요", "~예상돼요")
            - 투자 권유 절대 금지
            """;

    public static OpenAiChatRequest build(Map<String, List<NewsAnalysis>> sectorNewsMap) {
        String userContent = buildUserContent(sectorNewsMap);
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

    private static String buildUserContent(Map<String, List<NewsAnalysis>> sectorNewsMap) {
        StringBuilder sb = new StringBuilder("[섹터별 오늘 뉴스]\n\n");

        for (Map.Entry<String, List<NewsAnalysis>> entry : sectorNewsMap.entrySet()) {
            sb.append("[").append(entry.getKey()).append(" 섹터]\n");
            for (NewsAnalysis na : entry.getValue()) {
                String firstLine = na.getSummary().lines().findFirst().orElse(na.getSummary());
                sb.append("- [").append(na.getSentiment().name()).append("] ")
                        .append(na.getNews().getTitle())
                        .append(": ").append(firstLine).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private SectorAnalysisPrompt() {}
}
