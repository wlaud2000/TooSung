package com.project.toosung_back.domain.news.ai.dto;

import java.util.List;

public record NewsAnalysisResult(
        List<String> summary,
        List<String> keyPoints,
        String sentiment,
        String sentimentReason,
        boolean isStockRelevant
) {}
