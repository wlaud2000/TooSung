package com.project.toosung_back.domain.disclosure.converter;

import com.project.toosung_back.domain.disclosure.dto.response.DisclosureResDTO;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DisclosureConverter {

    public static DisclosureResDTO.DisclosureDetail toDisclosureDetail(Disclosure disclosure) {
        return DisclosureResDTO.DisclosureDetail.builder()
                .id(disclosure.getId())
                .disclosureType(disclosure.getDisclosureType())
                .url(disclosure.getUrl())
                .publishedAt(disclosure.getPublishedAt())
                .stockSymbol(disclosure.getStock().getSymbol())
                .stockName(disclosure.getStock().getName())
                .rawData(disclosure.getRawData())
                .aiSummary(disclosure.getAiSummary())
                .aiImpact(disclosure.getAiImpact())
                .aiInvestmentPoint(disclosure.getAiInvestmentPoint())
                .aiAnalyzedAt(disclosure.getAiAnalyzedAt())
                .build();
    }

    public static DisclosureResDTO.DisclosureItem toDisclosureItem(Disclosure disclosure) {
        String title = disclosure.getStock().getName() + " - " + disclosure.getDisclosureType();
        return DisclosureResDTO.DisclosureItem.builder()
                .disclosureId(disclosure.getId())
                .title(title)
                .disclosureType(disclosure.getDisclosureType())
                .submittedAt(disclosure.getPublishedAt())
                .build();
    }
}
