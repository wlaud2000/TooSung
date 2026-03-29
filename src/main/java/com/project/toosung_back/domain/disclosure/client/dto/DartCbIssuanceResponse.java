package com.project.toosung_back.domain.disclosure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART DS005 전환사채권발행결정 API 응답 DTO
 * endpoint: /api/cvbdIsDecsn.json
 */
public record DartCbIssuanceResponse(
        String status,
        String message,
        List<Item> list
) {
    public record Item(
            @JsonProperty("rcept_no") String rceptNo,
            @JsonProperty("corp_cls") String corpCls,
            @JsonProperty("corp_code") String corpCode,
            @JsonProperty("corp_name") String corpName,
            @JsonProperty("bd_tm") String bdTm,                   // 회차
            @JsonProperty("bd_knd") String bdKnd,                 // 사채종류
            @JsonProperty("bd_fca") String bdFca,                 // 사채권면총액
            @JsonProperty("bd_int") String bdInt,                 // 표면이자율
            @JsonProperty("bd_mtd") String bdMtd,                 // 사채만기일
            @JsonProperty("cnstkrt") String cnstkrt               // 전환비율
    ) {}
}
