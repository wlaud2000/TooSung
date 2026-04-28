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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EdgarApiClient {

    private static final String SUBMISSIONS_URL = "https://data.sec.gov/submissions/CIK%s.json";
    private static final String TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";

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
