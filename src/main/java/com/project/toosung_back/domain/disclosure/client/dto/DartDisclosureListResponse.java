package com.project.toosung_back.domain.disclosure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DartDisclosureListResponse(
        String status,
        String message,
        @JsonProperty("total_count") int totalCount,
        List<DartItem> list
) {
    public record DartItem(
            @JsonProperty("corp_cls") String corpCls,
            @JsonProperty("corp_name") String corpName,
            @JsonProperty("corp_code") String corpCode,
            @JsonProperty("stock_code") String stockCode,
            @JsonProperty("report_nm") String reportNm,
            @JsonProperty("rcept_no") String rceptNo,
            @JsonProperty("flr_nm") String flrNm,
            @JsonProperty("rcept_dt") String rceptDt,
            String rm
    ) {}
}
