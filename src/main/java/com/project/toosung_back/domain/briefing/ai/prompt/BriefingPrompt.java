package com.project.toosung_back.domain.briefing.ai.prompt;

import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.userinterest.entity.UserInterest;
import com.project.toosung_back.domain.userinterest.enums.InterestType;
import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;

import java.util.List;

public class BriefingPrompt {

    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 400;
    private static final double TEMPERATURE = 0.6;

    private static final String SYSTEM_PROMPT = """
            당신은 개인 투자자의 전용 주식 비서입니다.
            오늘의 뉴스·공시 분석 결과와 사용자의 과거 관심사를 바탕으로 자연스러운 브리핑을 생성합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "title": "오늘 브리핑을 한 줄로 요약한 제목",
              "summary": "자연스러운 비서 말투로 작성한 2~3문장 브리핑 본문",
              "newsIds": [브리핑 본문에서 실제로 참고한 뉴스 id 목록, 없으면 빈 배열]
            }

            규칙:
            - summary: 친근하고 자연스러운 비서 말투 사용 ("~이에요", "~있어요", "~보이네요")
            - summary: 과거 관심사가 제공된 경우 오늘 뉴스와 연결하는 문구 포함 (예: "지난번 관심 가졌던 HBM 관련 새 소식이에요"). 관심사가 없으면 생략
            - newsIds: 본문에서 직접 언급하거나 핵심 근거로 삼은 뉴스 id만 포함. 없으면 빈 배열
            - 투자 권유는 절대 하지 않음. 사실과 맥락만 전달
            """;

    public static OpenAiChatRequest build(
            List<NewsAnalysis> newsList,
            List<DisclosureAnalysis> disclosureList,
            List<UserInterest> topInterests
    ) {
        String userContent = buildUserContent(newsList, disclosureList, topInterests);
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

    private static String buildUserContent(
            List<NewsAnalysis> newsList,
            List<DisclosureAnalysis> disclosureList,
            List<UserInterest> topInterests
    ) {
        StringBuilder sb = new StringBuilder();

        if (!topInterests.isEmpty()) {
            sb.append("[사용자 과거 관심사]\n");
            for (UserInterest interest : topInterests) {
                String level = interestLevel(interest.getWeight());
                String typeLabel = interestTypeLabel(interest.getInterestType());
                sb.append("- ").append(interest.getTopic())
                        .append(" (").append(typeLabel).append(", ").append(level).append(")\n");
            }
        }

        if (!newsList.isEmpty()) {
            sb.append("\n[오늘 뉴스 분석]\n");
            for (NewsAnalysis na : newsList) {
                String firstSentence = na.getSummary().lines().findFirst().orElse(na.getSummary());
                sb.append("[id:").append(na.getNews().getId()).append("] ")
                        .append(na.getNews().getTitle())
                        .append(" / 감성: ").append(na.getSentiment().name())
                        .append(" / 요약: ").append(firstSentence)
                        .append("\n");
            }
        }

        if (!disclosureList.isEmpty()) {
            sb.append("\n[오늘 공시 분석]\n");
            for (DisclosureAnalysis da : disclosureList) {
                sb.append(da.getDisclosure().getStock().getName()).append(": ")
                        .append("[").append(da.getDisclosure().getDisclosureType()).append("] ")
                        .append(da.getSimpleSummary())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    private static String interestLevel(double weight) {
        if (weight >= 0.7) return "관심도 높음";
        if (weight >= 0.4) return "관심도 보통";
        return "관심도 낮음";
    }

    private static String interestTypeLabel(InterestType type) {
        return switch (type) {
            case STOCK -> "종목";
            case SECTOR -> "섹터";
            case KEYWORD -> "키워드";
        };
    }
}
