package com.project.toosung_back.domain.disclosure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.disclosure.ai.DisclosureAnalysisPrompt;
import com.project.toosung_back.domain.disclosure.ai.dto.DisclosureAnalysisResult;
import com.project.toosung_back.domain.disclosure.client.DartApiClient;
import com.project.toosung_back.domain.disclosure.client.EdgarApiClient;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.disclosure.entity.DisclosureSource;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureAnalysisService {

    private static final int BATCH_SIZE = 10;

    private final DisclosureRepository disclosureRepository;
    private final DisclosureAnalysisRepository disclosureAnalysisRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final FilingTextExtractor filingTextExtractor;
    private final DartApiClient dartApiClient;
    private final EdgarApiClient edgarApiClient;

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\((\\d{4})\\.");

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
        String rawData = disclosure.getRawData();

        if (rawData == null) {
            rawData = fetchRawContent(disclosure);
            if (rawData != null) {
                disclosure.updateRawData(rawData);
                disclosureRepository.save(disclosure);
            }
        }

        OpenAiChatRequest request = DisclosureAnalysisPrompt.build(
                disclosure.getDisclosureType(),
                disclosure.getStock().getName(),
                rawData
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

    private String fetchRawContent(Disclosure disclosure) {
        try {
            String type = disclosure.getDisclosureType();

            if (DisclosureSource.EDGAR.equals(disclosure.getSource())) {
                if ("10-K".equals(type) || "10-Q".equals(type)) {
                    String cik = disclosure.getStock().getEdgarCik();
                    if (cik != null) {
                        String financial = edgarApiClient.fetchFinancialSummary(cik, type);
                        if (financial != null) return financial;
                    }
                }
                return filingTextExtractor.extractFromEdgar(disclosure.getUrl());
            } else {
                if (isDartFinancialReport(type)) {
                    String corpCode = disclosure.getStock().getDartCorpCode();
                    if (corpCode != null) {
                        String financial = dartApiClient.fetchFinancialSummary(
                                corpCode, extractYear(type, disclosure.getPublishedAt()), toReprtCode(type));
                        if (financial != null) return financial;
                    }
                }
                return filingTextExtractor.extractFromDart(disclosure.getDartId());
            }
        } catch (Exception e) {
            log.warn("[DisclosureAnalysisService] 원문 fetch 실패 - disclosureId={}, error={}",
                    disclosure.getId(), e.getMessage());
            return null;
        }
    }

    private boolean isDartFinancialReport(String type) {
        return type.contains("사업보고서") || type.contains("분기보고서") || type.contains("반기보고서");
    }

    private String extractYear(String type, LocalDateTime publishedAt) {
        Matcher m = YEAR_PATTERN.matcher(type);
        return m.find() ? m.group(1) : String.valueOf(publishedAt.getYear());
    }

    private String toReprtCode(String type) {
        if (type.contains("사업보고서")) return "11011";
        if (type.contains("반기보고서")) return "11012";
        if (type.contains("분기보고서")) {
            if (type.contains(".09") || type.contains(".12")) return "11014";
            return "11013";
        }
        return "11011";
    }
}
