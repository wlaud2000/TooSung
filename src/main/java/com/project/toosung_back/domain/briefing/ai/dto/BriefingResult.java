package com.project.toosung_back.domain.briefing.ai.dto;

import java.util.List;

public record BriefingResult(
        String title,
        String sumary,
        List<Long> newsIds
) {
}
