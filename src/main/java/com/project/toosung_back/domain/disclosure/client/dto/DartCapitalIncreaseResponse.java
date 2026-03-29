package com.project.toosung_back.domain.disclosure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART DS005 유상증자결정 API 응답 DTO
 * endpoint: /api/piicDecsn.json
 */
public record DartCapitalIncreaseResponse(
        String status,
        String message,
        List<Item> list
) {
    public record Item(
            @JsonProperty("rcept_no") String rceptNo,
            @JsonProperty("corp_cls") String corpCls,
            @JsonProperty("corp_code") String corpCode,
            @JsonProperty("corp_name") String corpName,
            @JsonProperty("isu_mth") String isuMth,               // 발행방법
            @JsonProperty("new_isu_stock_qty") String newIsuStockQty, // 신주발행수
            @JsonProperty("fv_ps") String fvPs,                   // 액면가
            @JsonProperty("isu_price") String isuPrice,           // 발행가액
            @JsonProperty("isu_amt") String isuAmt,               // 증자금액
            @JsonProperty("iss_rc") String issRc,                 // 발행이유
            @JsonProperty("subs_date") String subsDate,           // 청약일
            @JsonProperty("pay_date") String payDate              // 납입일
    ) {}
}
