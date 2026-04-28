package com.project.toosung_back.domain.disclosure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record EdgarSubmissionsResponse(
        @JsonProperty("cik") String cik,
        @JsonProperty("name") String name,
        @JsonProperty("filings") Filings filings
) {
    public record Filings(
            @JsonProperty("recent") Recent recent
    ) {}

    public record Recent(
            @JsonProperty("accessionNumber") List<String> accessionNumbers,
            @JsonProperty("filingDate") List<String> filingDates,
            @JsonProperty("form") List<String> forms,
            @JsonProperty("primaryDocument") List<String> primaryDocuments
    ) {}

    public record FilingItem(
            String accessionNumber,
            String filingDate,
            String form,
            String primaryDocument
    ) {}

    public List<FilingItem> toFilingItems() {
        if (filings == null || filings.recent() == null) return List.of();
        Recent r = filings.recent();
        int size = r.accessionNumbers() != null ? r.accessionNumbers().size() : 0;
        List<FilingItem> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String primaryDoc = (r.primaryDocuments() != null && r.primaryDocuments().size() > i)
                    ? r.primaryDocuments().get(i) : "";
            items.add(new FilingItem(
                    r.accessionNumbers().get(i),
                    r.filingDates().get(i),
                    r.forms().get(i),
                    primaryDoc
            ));
        }
        return items;
    }
}
