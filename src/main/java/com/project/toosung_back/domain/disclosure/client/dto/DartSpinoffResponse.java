package com.project.toosung_back.domain.disclosure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART DS005 회사분할결정 API 응답 DTO
 * endpoint: /api/dssDecsn.json
 */
public record DartSpinoffResponse(
        String status,
        String message,
        List<Item> list
) {
    public record Item(
            @JsonProperty("rcept_no") String rceptNo,
            @JsonProperty("corp_cls") String corpCls,
            @JsonProperty("corp_code") String corpCode,
            @JsonProperty("corp_name") String corpName,
            @JsonProperty("dvss_mth") String dvssMth,             // 분할방법
            @JsonProperty("dvss_sc") String dvssSc,               // 분할이유
            @JsonProperty("dvss_dt") String dvssDt                // 분할기일
    ) {}
}
