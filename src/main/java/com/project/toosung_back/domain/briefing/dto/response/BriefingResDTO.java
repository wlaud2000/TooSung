package com.project.toosung_back.domain.briefing.dto.response;

import java.util.List;

public class BriefingResDTO {

    public record BriefingDetail(
            String title,
            String summary,
            List<Long> newsIds,
            List<Long> disclosureIds
    ) {
    }
}
