package com.project.toosung_back.domain.disclosure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.disclosure.client.dto.EdgarSubmissionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EdgarApiClient {

    private static final String SUBMISSIONS_URL = "https://data.sec.gov/submissions/CIK%s.json";
    private static final String TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";
    private static final String CONCEPT_URL = "https://data.sec.gov/api/xbrl/companyconcept/CIK%s/us-gaap/%s.json";
    private static final long RATE_LIMIT_DELAY_MS = 150;

    private static final List<String> REVENUE_CONCEPTS = List.of(
            "Revenues",
            "RevenueFromContractWithCustomerExcludingAssessedTax",
            "SalesRevenueNet"
    );
    private static final List<String> OTHER_CONCEPTS = List.of(
            "OperatingIncomeLoss",
            "NetIncomeLoss",
            "EarningsPerShareBasic"
    );

    @Qualifier("edgarWebClient")
    private final WebClient edgarWebClient;

    private final ObjectMapper objectMapper;

    /**
     * CIK로 기업의 최근 공시 목록 조회
     * @param cik 10자리 zero-padded CIK
     */
    public EdgarSubmissionsResponse fetchSubmissions(String cik) {
        try {
            String json = edgarWebClient.get()
                    .uri(String.format(SUBMISSIONS_URL, cik))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (json == null) return null;
            return objectMapper.readValue(json, EdgarSubmissionsResponse.class);
        } catch (Exception e) {
            log.error("[EdgarApiClient] submissions 조회 실패: cik={}, error={}", cik, e.getMessage());
            return null;
        }
    }

    /**
     * 10-K / 10-Q 핵심 재무 지표 조회 (XBRL companyconcept API)
     * @param cik      10자리 zero-padded CIK
     * @param formType "10-K" 또는 "10-Q"
     */
    public String fetchFinancialSummary(String cik, String formType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Financial Summary (").append(formType).append(")\n\n");

        // Revenue (여러 개념명 시도)
        for (String concept : REVENUE_CONCEPTS) {
            String value = fetchLatestConceptValue(cik, concept, formType);
            rateLimitSleep();
            if (value != null) {
                sb.append("Revenue: ").append(value).append("\n");
                break;
            }
        }

        // 나머지 핵심 지표
        for (String concept : OTHER_CONCEPTS) {
            String value = fetchLatestConceptValue(cik, concept, formType);
            rateLimitSleep();
            if (value != null) {
                sb.append(concept).append(": ").append(value).append("\n");
            }
        }

        String result = sb.toString().trim();
        return result.equals("Financial Summary (" + formType + ")") ? null : result;
    }

    private String fetchLatestConceptValue(String cik, String concept, String formType) {
        try {
            String json = edgarWebClient.get()
                    .uri(String.format(CONCEPT_URL, cik, concept))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return null;

            JsonNode root = objectMapper.readTree(json);
            JsonNode usdItems = root.path("units").path("USD");
            if (usdItems.isMissingNode()) return null;

            JsonNode latest = null;
            for (JsonNode item : usdItems) {
                if (!formType.equals(item.path("form").asText())) continue;
                if (latest == null || item.path("end").asText().compareTo(latest.path("end").asText()) > 0) {
                    latest = item;
                }
            }

            if (latest == null) return null;

            long val = latest.path("val").asLong();
            String end = latest.path("end").asText();
            return formatUsd(val) + " (as of " + end + ")";

        } catch (Exception e) {
            return null;
        }
    }

    private String formatUsd(long value) {
        long abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 1_000_000_000L) return sign + String.format("$%.1fB", abs / 1_000_000_000.0);
        if (abs >= 1_000_000L) return sign + String.format("$%.1fM", abs / 1_000_000.0);
        return sign + "$" + abs;
    }

    private void rateLimitSleep() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * SEC 전체 ticker → CIK 매핑 다운로드
     * @return ticker(대문자) → cik(10자리 zero-padded) 맵
     */
    public Map<String, String> fetchTickerToCikMap() {
        try {
            String json = edgarWebClient.get()
                    .uri(TICKERS_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return Collections.emptyMap();

            JsonNode root = objectMapper.readTree(json);
            Map<String, String> result = new HashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode item = entry.getValue();
                String ticker = item.path("ticker").asText().toUpperCase();
                long cikLong = item.path("cik_str").asLong();
                if (!ticker.isBlank() && cikLong > 0) {
                    result.put(ticker, String.format("%010d", cikLong));
                }
            });
            return result;

        } catch (Exception e) {
            log.error("[EdgarApiClient] ticker-CIK 매핑 다운로드 실패: error={}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
