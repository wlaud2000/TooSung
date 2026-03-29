package com.project.toosung_back.domain.disclosure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART DS005 자기주식취득결정 API 응답 DTO
 * endpoint: /api/tcbDecsn.json
 */
public record DartTreasuryStockResponse(
        String status,
        String message,
        List<Item> list
) {
    public record Item(
            @JsonProperty("rcept_no") String rceptNo,
            @JsonProperty("corp_cls") String corpCls,
            @JsonProperty("corp_code") String corpCode,
            @JsonProperty("corp_name") String corpName,
            @JsonProperty("aqstk_mth") String aqstkMth,           // 취득방법
            @JsonProperty("aqstk_sc") String aqstkSc,             // 취득이유
            @JsonProperty("bg_acq_de") String bgAcqDe,            // 취득시작일
            @JsonProperty("en_acq_de") String enAcqDe,            // 취득종료일
            @JsonProperty("aqstk_rt") String aqstkRt              // 취득비율
    ) {}
}
