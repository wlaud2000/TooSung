package com.project.toosung_back.domain.disclosure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART DS005 합병결정 API 응답 DTO
 * endpoint: /api/mrgrDecsn.json
 */
public record DartMergerResponse(
        String status,
        String message,
        List<Item> list
) {
    public record Item(
            @JsonProperty("rcept_no") String rceptNo,
            @JsonProperty("corp_cls") String corpCls,
            @JsonProperty("corp_code") String corpCode,
            @JsonProperty("corp_name") String corpName,
            @JsonProperty("mgr_mth") String mgrMth,               // 합병방법
            @JsonProperty("mgr_sc") String mgrSc,                 // 합병이유
            @JsonProperty("mgr_rt") String mgrRt,                 // 합병비율
            @JsonProperty("ptr_corp_name") String ptrCorpName,    // 합병상대방 회사명
            @JsonProperty("mgr_shd") String mgrShd                // 합병일정
    ) {}
}
