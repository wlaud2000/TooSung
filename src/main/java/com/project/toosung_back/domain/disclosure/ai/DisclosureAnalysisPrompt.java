package com.project.toosung_back.domain.disclosure.ai;

import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;

import java.util.List;
import java.util.Map;

public class DisclosureAnalysisPrompt {

    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 400;
    private static final double TEMPERATURE = 0.3;
    private static final int RAW_DATA_MAX_LENGTH = 1000;

    private static final String EARNINGS_SYSTEM_PROMPT = """
            당신은 주식 투자 전문 애널리스트입니다.
            실적 보고서 공시를 분석하여 투자자에게 유용한 정보를 제공합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "simpleSummary": "이 실적 보고서의 핵심을 일반 투자자가 이해할 수 있는 언어로 3문장 이내로 설명",
              "investmentPoint": "매출·영업이익 증감 방향과 주가에 미치는 영향을 2문장으로 설명"
            }

            규칙:
            - simpleSummary: 매출·영업이익 등 주요 수치를 전문 용어 없이 쉬운 말로 번역
            - investmentPoint: 전년 동기 대비 증감 방향, 수익성 변화, 주가 영향 방향(긍정/부정/중립)을 포함
            """;

    private static final String CAPITAL_SYSTEM_PROMPT = """
            당신은 주식 투자 전문 애널리스트입니다.
            자본 조달 공시(유상증자·전환사채)를 분석하여 투자자에게 유용한 정보를 제공합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "simpleSummary": "이 공시의 핵심을 일반 투자자가 이해할 수 있는 언어로 3문장 이내로 설명",
              "investmentPoint": "주식 수 증가(희석 효과)와 기존 주주 가치에 미치는 영향을 2문장으로 설명"
            }

            규칙:
            - simpleSummary: 자금 조달 목적과 규모를 전문 용어 없이 쉬운 말로 번역
            - investmentPoint: 신주 발행에 따른 주식 수 증가율(희석 효과), 기존 주주 가치 훼손 여부, 조달 목적의 적정성을 포함
            """;

    private static final String RESTRUCTURING_SYSTEM_PROMPT = """
            당신은 주식 투자 전문 애널리스트입니다.
            기업 구조 변화 공시(합병·분할)를 분석하여 투자자에게 유용한 정보를 제공합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "simpleSummary": "이 공시의 핵심을 일반 투자자가 이해할 수 있는 언어로 3문장 이내로 설명",
              "investmentPoint": "합병·분할 후 기업 가치 변화와 주가에 미치는 영향을 2문장으로 설명"
            }

            규칙:
            - simpleSummary: 합병·분할 상대방과 목적을 전문 용어 없이 쉬운 말로 번역
            - investmentPoint: 합병 시너지 또는 분할 후 사업 집중도 변화, 주가 영향 방향(긍정/부정/중립)을 포함
            """;

    private static final String TREASURY_SYSTEM_PROMPT = """
            당신은 주식 투자 전문 애널리스트입니다.
            자기주식 취득·처분 공시를 분석하여 투자자에게 유용한 정보를 제공합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "simpleSummary": "이 공시의 핵심을 일반 투자자가 이해할 수 있는 언어로 3문장 이내로 설명",
              "investmentPoint": "주가 방어 효과 또는 주식 처분에 따른 영향을 2문장으로 설명"
            }

            규칙:
            - simpleSummary: 자기주식 취득·처분 목적과 규모를 전문 용어 없이 쉬운 말로 번역
            - investmentPoint: 주가 방어 효과(취득) 또는 희석 영향(처분), 시장에 보내는 신호(경영진 자신감 등)를 포함
            """;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 주식 투자 전문 애널리스트입니다.
            공시를 분석하여 투자자에게 유용한 정보를 제공합니다.
            아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.

            {
              "simpleSummary": "이 공시의 핵심을 일반 투자자가 이해할 수 있는 언어로 3문장 이내로 설명",
              "investmentPoint": "이 공시가 주가에 미치는 영향을 2문장으로 설명"
            }

            규칙:
            - simpleSummary: 공시 내용을 전문 용어 없이 쉬운 말로 번역
            - investmentPoint: 주가 영향 방향(긍정/부정/중립)과 투자자가 주목해야 할 포인트를 포함
            """;

    private static final Map<String, String> KEYWORD_TO_SYSTEM_PROMPT = Map.of(
            "사업보고서", EARNINGS_SYSTEM_PROMPT,
            "분기보고서", EARNINGS_SYSTEM_PROMPT,
            "반기보고서", EARNINGS_SYSTEM_PROMPT,
            "유상증자", CAPITAL_SYSTEM_PROMPT,
            "전환사채", CAPITAL_SYSTEM_PROMPT,
            "합병", RESTRUCTURING_SYSTEM_PROMPT,
            "분할", RESTRUCTURING_SYSTEM_PROMPT,
            "자기주식", TREASURY_SYSTEM_PROMPT
    );

    private DisclosureAnalysisPrompt() {}

    public static OpenAiChatRequest build(String disclosureType, String stockName, String rawData) {
        String systemPrompt = findSystemPrompt(disclosureType);
        String userContent = "공시 유형: " + disclosureType
                + "\n종목명: " + stockName
                + "\n공시 데이터: " + truncateRawData(rawData);

        return new OpenAiChatRequest(
                MODEL,
                List.of(
                        new OpenAiChatRequest.Message("system", systemPrompt),
                        new OpenAiChatRequest.Message("user", userContent)
                ),
                MAX_TOKENS,
                TEMPERATURE
        );
    }

    private static String findSystemPrompt(String disclosureType) {
        return KEYWORD_TO_SYSTEM_PROMPT.entrySet().stream()
                .filter(e -> disclosureType.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(DEFAULT_SYSTEM_PROMPT);
    }

    private static String truncateRawData(String rawData) {
        if (rawData == null || rawData.isBlank()) {
            return "공시 세부 데이터 없음";
        }
        return rawData.length() > RAW_DATA_MAX_LENGTH
                ? rawData.substring(0, RAW_DATA_MAX_LENGTH) + "...(이하 생략)"
                : rawData;
    }
}
