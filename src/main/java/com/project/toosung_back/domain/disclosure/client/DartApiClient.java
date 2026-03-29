package com.project.toosung_back.domain.disclosure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.disclosure.client.dto.DartDisclosureListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DartApiClient {

    @Qualifier("dartWebClient")
    private final WebClient dartWebClient;

    private final ObjectMapper objectMapper;

    @Value("${dart.api-key}")
    private String apiKey;

    /**
     * DS001 공시검색 - 날짜 및 공시유형으로 공시 목록 조회
     *
     * @param pblntfTy 공시유형 (B: 주요사항보고서, A: 정기공시)
     * @param date     조회 날짜 (YYYYMMDD)
     */
    public List<DartDisclosureListResponse.DartItem> fetchDisclosureList(String pblntfTy, String date) {
        try {
            DartDisclosureListResponse response = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/list.json")
                            .queryParam("crtfc_key", apiKey)
                            .queryParam("bgn_de", date)
                            .queryParam("end_de", date)
                            .queryParam("pblntf_ty", pblntfTy)
                            .queryParam("page_count", 100)
                            .build())
                    .retrieve()
                    .bodyToMono(DartDisclosureListResponse.class)
                    .block();

            if (response == null || !"000".equals(response.status())) {
                String status = response != null ? response.status() : "null";
                // "020": 조회 결과 없음 (정상 케이스)
                if (!"020".equals(status)) {
                    log.warn("[DartApiClient] DS001 비정상 응답: pblntfTy={}, status={}", pblntfTy, status);
                }
                return Collections.emptyList();
            }

            if (response.totalCount() > 100) {
                log.warn("[DartApiClient] DS001 total_count({}) > 100 - 초과분 누락 가능: pblntfTy={}", response.totalCount(), pblntfTy);
            }

            return response.list() != null ? response.list() : Collections.emptyList();

        } catch (Exception e) {
            log.error("[DartApiClient] DS001 수집 실패: pblntfTy={}, error={}", pblntfTy, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * DS005 세부 공시 API - 공시 상세 데이터를 raw JSON 문자열로 반환
     *
     * @param endpoint  DS005 엔드포인트 (예: /api/piicDecsn.json)
     * @param corpCode  DART 8자리 고유번호
     * @param date      조회 날짜 (YYYYMMDD)
     * @return status가 "000"인 경우 raw JSON 문자열, 그 외 null
     */
    public String fetchDisclosureDetailRaw(String endpoint, String corpCode, String date) {
        try {
            JsonNode node = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("crtfc_key", apiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bgn_de", date)
                            .queryParam("end_de", date)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (node == null) return null;

            String status = node.path("status").asText();
            if (!"000".equals(status)) {
                if (!"020".equals(status)) {
                    log.warn("[DartApiClient] DS005 비정상 응답: endpoint={}, corpCode={}, status={}", endpoint, corpCode, status);
                }
                return null;
            }

            return objectMapper.writeValueAsString(node);

        } catch (Exception e) {
            log.error("[DartApiClient] DS005 수집 실패: endpoint={}, corpCode={}, error={}", endpoint, corpCode, e.getMessage());
            return null;
        }
    }

    /**
     * DART 전체 기업 코드 목록을 ZIP 파일로 다운로드
     * 응답: CORPCODE.xml을 포함한 ZIP 바이너리
     */
    public byte[] fetchCorpCodeZip() {
        try {
            return dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/corpCode.xml")
                            .queryParam("crtfc_key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("[DartApiClient] corpCode.xml 다운로드 실패: error={}", e.getMessage());
            return null;
        }
    }
}
