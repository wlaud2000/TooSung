package com.project.toosung_back.domain.disclosure.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

public class DisclosureResDTO {

    @Builder
    public record DisclosureDetail(
            Long id,
            String disclosureType,
            String url,
            LocalDateTime publishedAt,
            String stockSymbol,
            String stockName,
            String rawData,
            String aiSummary,
            String aiImpact,
            String aiInvestmentPoint,
            LocalDateTime aiAnalyzedAt
    ) {
    }
}
