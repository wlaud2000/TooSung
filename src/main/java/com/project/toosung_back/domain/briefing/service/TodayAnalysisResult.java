package com.project.toosung_back.domain.briefing.service;

import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;

import java.util.List;

// newsList와 disclosureList를 하나의 객체로 묶어 TodayAnalysisService로 전달 -> 호출부 간 전달을 단순화
record TodayAnalysisResult(
        List<NewsAnalysis> newsList,
        List<DisclosureAnalysis> disclosureList
) {}