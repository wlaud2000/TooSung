package com.project.toosung_back.domain.disclosure.converter;

import com.project.toosung_back.domain.disclosure.dto.response.DisclosureResDTO;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DisclosureConverter {

    public static DisclosureResDTO.DisclosureDetail toDisclosureDetail(
            Disclosure disclosure,
            Optional<DisclosureAnalysis> analysis
    ) {
        return DisclosureResDTO.DisclosureDetail.builder()
                .id(disclosure.getId())
                .disclosureType(disclosure.getDisclosureType())
                .url(disclosure.getUrl())
                .publishedAt(disclosure.getPublishedAt())
                .stockSymbol(disclosure.getStock().getSymbol())
                .stockName(disclosure.getStock().getName())
                .rawData(disclosure.getRawData())
                .simpleSummary(analysis.map(DisclosureAnalysis::getSimpleSummary).orElse(null))
                .investmentPoint(analysis.map(DisclosureAnalysis::getInvestmentPoint).orElse(null))
                .analyzedAt(analysis.map(DisclosureAnalysis::getAnalyzedAt).orElse(null))
                .build();
    }

    public static DisclosureResDTO.DisclosureItem toDisclosureItem(Disclosure disclosure, Optional<DisclosureAnalysis> analysis) {
        String title = disclosure.getStock().getName() + " - " + disclosure.getDisclosureType();
        return DisclosureResDTO.DisclosureItem.builder()
                .disclosureId(disclosure.getId())
                .title(title)
                .disclosureType(disclosure.getDisclosureType())
                .submittedAt(disclosure.getPublishedAt())
                .url(disclosure.getUrl())
                .simpleSummary(analysis.map(DisclosureAnalysis::getSimpleSummary).orElse(null))
                .investmentPoint(analysis.map(DisclosureAnalysis::getInvestmentPoint).orElse(null))
                .build();
    }
}
