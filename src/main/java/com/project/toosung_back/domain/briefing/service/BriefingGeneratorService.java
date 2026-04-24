package com.project.toosung_back.domain.briefing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.briefing.ai.dto.BriefingResult;
import com.project.toosung_back.domain.briefing.ai.prompt.BriefingPrompt;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.userinterest.entity.UserInterest;
import com.project.toosung_back.domain.userinterest.repository.UserInterestRepository;
import com.project.toosung_back.global.openai.client.OpenAiClient;
import com.project.toosung_back.global.openai.dto.OpenAiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingGeneratorService {

    private static final int TOP_INTERESTS = 5;

    private final TodayAnalysisService todayAnalysisService;
    private final UserInterestRepository userInterestRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public BriefingResult generate(Long memberId) throws Exception {
        TodayAnalysisResult todayResult = todayAnalysisService.aggregate(memberId);

        if (todayResult.newsList().isEmpty() && todayResult.disclosureList().isEmpty()) {
            log.info("[BriefingGeneratorService] 오늘 분석된 항목 없음 - memberId={}", memberId);
            return emptyResult();
        }

        List<UserInterest> topInterests = userInterestRepository
                .findByMember_IdOrderByWeightDesc(memberId, PageRequest.of(0, TOP_INTERESTS));

        OpenAiChatResponse response = openAiClient.chat(
                BriefingPrompt.build(todayResult.newsList(), todayResult.disclosureList(), topInterests));

        String rawJson = response.choices().get(0).message().content();
        BriefingResult raw = objectMapper.readValue(rawJson, BriefingResult.class);

        List<Long> disclosureIds = todayResult.disclosureList().stream()
                .map(da -> da.getDisclosure().getId())
                .toList();

        return validateAndBuild(raw, todayResult.newsList(), disclosureIds);
    }

    private BriefingResult validateAndBuild(BriefingResult raw, List<NewsAnalysis> newsList, List<Long> disclosureIds) {
        if (raw.newsIds() == null || raw.newsIds().isEmpty()) {
            return new BriefingResult(raw.title(), raw.summary(), List.of(), disclosureIds);
        }

        Set<Long> validIds = newsList.stream()
                .map(na -> na.getNews().getId())
                .collect(Collectors.toSet());

        List<Long> sanitized = raw.newsIds().stream()
                .filter(validIds::contains)
                .toList();

        if (sanitized.size() != raw.newsIds().size()) {
            log.warn("[BriefingGeneratorService] LLM hallucinate newsId 감지 - 원본 {}건 → 필터링 후 {}건",
                    raw.newsIds().size(), sanitized.size());
        }

        return new BriefingResult(raw.title(), raw.summary(), sanitized, disclosureIds);
    }

    private BriefingResult emptyResult() {
        return new BriefingResult(
                "오늘 브리핑을 준비 중입니다.",
                "아직 분석된 항목이 없어요. 잠시 후 다시 확인해주세요.",
                List.of(),
                List.of()
        );
    }
}
