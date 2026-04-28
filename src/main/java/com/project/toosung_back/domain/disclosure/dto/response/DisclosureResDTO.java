package com.project.toosung_back.domain.disclosure.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

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
            String simpleSummary,
            String investmentPoint,
            LocalDateTime analyzedAt
    ) {
    }

    @Builder
    public record DisclosureItem(
            Long disclosureId,
            String title,
            String disclosureType,
            LocalDateTime submittedAt,
            String url,
            String simpleSummary,
            String investmentPoint
    ) {}

    @Builder
    public record DisclosureList(
            List<DisclosureItem> items,
            Long nextCursor,
            boolean hasNext
    ) {}
}
