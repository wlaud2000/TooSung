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

import java.util.ArrayList;
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
        List<DartDisclosureListResponse.DartItem> allItems = new ArrayList<>();
        int pageNo = 1;

        try {
            while (true) {
                final int currentPage = pageNo;
                DartDisclosureListResponse response = dartWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/list.json")
                                .queryParam("crtfc_key", apiKey)
                                .queryParam("bgn_de", date)
                                .queryParam("end_de", date)
                                .queryParam("pblntf_ty", pblntfTy)
                                .queryParam("page_count", 100)
                                .queryParam("page_no", currentPage)
                                .build())
                        .retrieve()
                        .bodyToMono(DartDisclosureListResponse.class)
                        .block();

                if (response == null || !"000".equals(response.status())) {
                    String status = response != null ? response.status() : "null";
                    if (!"020".equals(status) && pageNo == 1) {
                        log.warn("[DartApiClient] DS001 비정상 응답: pblntfTy={}, status={}", pblntfTy, status);
                    }
                    break;
                }

                List<DartDisclosureListResponse.DartItem> pageItems = response.list() != null ? response.list() : Collections.emptyList();
                allItems.addAll(pageItems);

                int totalCount = response.totalCount() != null ? response.totalCount() : 0;
                if (allItems.size() >= totalCount || pageItems.isEmpty()) {
                    break;
                }
                pageNo++;
            }

            log.info("[DartApiClient] DS001 수집 완료: pblntfTy={}, date={}, 총={}건", pblntfTy, date, allItems.size());
            return allItems;

        } catch (Exception e) {
            log.error("[DartApiClient] DS001 수집 실패: pblntfTy={}, error={}", pblntfTy, e.getMessage());
            return allItems;
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
            String json = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("crtfc_key", apiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bgn_de", date)
                            .queryParam("end_de", date)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return null;

            JsonNode node = objectMapper.readTree(json);
            String status = node.path("status").asText();
            if (!"000".equals(status)) {
                if (!"020".equals(status)) {
                    log.warn("[DartApiClient] DS005 비정상 응답: endpoint={}, corpCode={}, status={}", endpoint, corpCode, status);
                }
                return null;
            }

            return json;

        } catch (Exception e) {
            log.error("[DartApiClient] DS005 수집 실패: endpoint={}, corpCode={}, error={}", endpoint, corpCode, e.getMessage());
            return null;
        }
    }

    /**
     * 단일 회사 주요 재무 항목 조회 (손익계산서 기준)
     * @param corpCode  DART 8자리 고유번호
     * @param bsnsYear  사업연도 (YYYY)
     * @param reprtCode 보고서 코드 (11011: 사업보고서, 11012: 반기, 11013: 1분기, 11014: 3분기)
     */
    public String fetchFinancialSummary(String corpCode, String bsnsYear, String reprtCode) {
        try {
            String json = dartWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/fnlttSinglAcnt.json")
                            .queryParam("crtfc_key", apiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bsns_year", bsnsYear)
                            .queryParam("reprt_code", reprtCode)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null) return null;

            JsonNode root = objectMapper.readTree(json);
            if (!"000".equals(root.path("status").asText())) {
                log.warn("[DartApiClient] 재무정보 비정상 응답: corpCode={}, status={}", corpCode, root.path("status").asText());
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("재무제표 (").append(bsnsYear).append(")\n\n");

            for (JsonNode item : root.path("list")) {
                if (!"IS".equals(item.path("sj_div").asText())) continue;

                String accountNm = item.path("account_nm").asText();
                if (!isKeyFinancialAccount(accountNm)) continue;

                String current = formatKrw(item.path("thstrm_amount").asText());
                String previous = formatKrw(item.path("frmtrm_amount").asText());
                sb.append(accountNm).append(": ").append(current)
                        .append(" (전기: ").append(previous).append(")\n");
            }

            String result = sb.toString().trim();
            return result.equals("재무제표 (" + bsnsYear + ")") ? null : result;

        } catch (Exception e) {
            log.error("[DartApiClient] 재무정보 수집 실패: corpCode={}, error={}", corpCode, e.getMessage());
            return null;
        }
    }

    private boolean isKeyFinancialAccount(String accountNm) {
        return accountNm.contains("매출") || accountNm.contains("영업이익")
                || accountNm.contains("당기순이익") || accountNm.contains("순이익");
    }

    private String formatKrw(String amount) {
        try {
            long value = Long.parseLong(amount.replace(",", ""));
            long abs = Math.abs(value);
            String sign = value < 0 ? "-" : "";
            if (abs >= 1_000_000_000_000L) return sign + String.format("%.1f조원", abs / 1_000_000_000_000.0);
            if (abs >= 100_000_000L) return sign + String.format("%.0f억원", abs / 100_000_000.0);
            return amount + "원";
        } catch (NumberFormatException e) {
            return amount;
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
