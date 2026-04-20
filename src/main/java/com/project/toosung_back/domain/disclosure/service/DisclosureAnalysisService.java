package com.project.toosung_back.domain.disclosure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.disclosure.ai.DisclosureAnalysisPrompt;
import com.project.toosung_back.domain.disclosure.ai.dto.DisclosureAnalysisResult;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.disclosure.repository.DisclosureAnalysisRepository;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.global.openai.client.OpenAiClient;
import com.project.toosung_back.global.openai.dto.OpenAiChatRequest;
import com.project.toosung_back.global.openai.dto.OpenAiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureAnalysisService {

    private static final int BATCH_SIZE = 10;

    private final DisclosureRepository disclosureRepository;
    private final DisclosureAnalysisRepository disclosureAnalysisRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @Async("disclosureAnalysisExecutor")
    public void analyzeUnanalyzedDisclosures() {
        List<Disclosure> disclosures = disclosureRepository.findUnanalyzedDisclosures(PageRequest.of(0, BATCH_SIZE));
        if (disclosures.isEmpty()) {
            log.info("[DisclosureAnalysisService] 분석 대상 공시 없음");
            return;
        }

        log.info("[DisclosureAnalysisService] 분석 시작 - {}건", disclosures.size());

        int successCount = 0;
        int failCount = 0;

        for (Disclosure disclosure : disclosures) {
            try {
                analyzeAndSave(disclosure);
                successCount++;
            } catch (Exception e) {
                log.error("[DisclosureAnalysisService] 분석 실패 - disclosureId={}, type={}, error={}",
                        disclosure.getId(), disclosure.getDisclosureType(), e.getMessage());
                failCount++;
            }
        }

        log.info("[DisclosureAnalysisService] 분석 완료 - 성공: {}건, 실패: {}건", successCount, failCount);
    }

    private void analyzeAndSave(Disclosure disclosure) throws Exception {
        OpenAiChatRequest request = DisclosureAnalysisPrompt.build(
                disclosure.getDisclosureType(),
                disclosure.getStock().getName(),
                disclosure.getRawData()
        );
        OpenAiChatResponse response = openAiClient.chat(request);

        String rawJson = response.choices().get(0).message().content();
        DisclosureAnalysisResult result = objectMapper.readValue(rawJson, DisclosureAnalysisResult.class);

        DisclosureAnalysis analysis = DisclosureAnalysis.builder()
                .disclosure(disclosure)
                .simpleSummary(result.simpleSummary())
                .investmentPoint(result.investmentPoint())
                .analyzedAt(LocalDateTime.now())
                .build();

        disclosureAnalysisRepository.save(analysis);

        log.info("[DisclosureAnalysisService] 분석 저장 완료 - disclosureId={}, type={}",
                disclosure.getId(), disclosure.getDisclosureType());
    }
}
