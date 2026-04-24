package com.project.toosung_back.domain.briefing.dto.response;

import java.util.List;

public class BriefingSourceResDTO {

    public static final String TYPE_NEWS = "NEWS";
    public static final String TYPE_DISCLOSURE = "DISCLOSURE";

    public record SourceItem(
            Long id,
            String title,
            String sentiment,
            String url,
            String type
    ) {
    }

    public record SourceList(
            List<SourceItem> items
    ) {
    }
}