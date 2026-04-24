package com.project.toosung_back.domain.briefing.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.briefing.ai.dto.BriefingResult;
import com.project.toosung_back.domain.briefing.dto.response.BriefingResDTO;
import com.project.toosung_back.domain.briefing.dto.response.BriefingSourceResDTO;
import com.project.toosung_back.domain.briefing.service.BriefingGeneratorService;
import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.disclosure.repository.DisclosureAnalysisRepository;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingQueryService {

    private static final String CACHE_KEY_PREFIX = "briefing:";

    private final BriefingGeneratorService briefingGeneratorService;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final DisclosureAnalysisRepository disclosureAnalysisRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public BriefingResDTO.BriefingDetail getTodayBriefing(Long memberId) {
        String cacheKey = CACHE_KEY_PREFIX + memberId;

        if (redisUtil.hasKey(cacheKey)) {
            String cached = redisUtil.get(cacheKey);
            try {
                return objectMapper.readValue(cached, BriefingResDTO.BriefingDetail.class);
            } catch (JsonProcessingException e) {
                log.warn("[BriefingQueryService] 캐시 역직렬화 실패, 재생성 - memberId={}", memberId);
                redisUtil.delete(cacheKey);
            }
        }

        try {
            BriefingResult result = briefingGeneratorService.generate(memberId);
            BriefingResDTO.BriefingDetail dto = toDetail(result);
            cacheUntilMidnight(cacheKey, dto);
            return dto;
        } catch (Exception e) {
            log.error("[BriefingQueryService] 브리핑 생성 실패 - memberId={}, error={}", memberId, e.getMessage());
            return emptyDetail();
        }
    }

    public BriefingSourceResDTO.SourceList getSources(Long memberId) {
        BriefingResDTO.BriefingDetail detail = getTodayBriefing(memberId);

        List<Long> newsIds = detail.newsIds() != null ? detail.newsIds() : List.of();
        List<Long> disclosureIds = detail.disclosureIds() != null ? detail.disclosureIds() : List.of();

        if (newsIds.isEmpty() && disclosureIds.isEmpty()) {
            return new BriefingSourceResDTO.SourceList(List.of());
        }

        List<BriefingSourceResDTO.SourceItem> items = new ArrayList<>();

        if (!newsIds.isEmpty()) {
            newsAnalysisRepository.findWithNewsByNewsIdIn(newsIds).stream()
                    .map(na -> new BriefingSourceResDTO.SourceItem(
                            na.getNews().getId(),
                            na.getNews().getTitle(),
                            na.getSentiment().name(),
                            na.getNews().getUrl(),
                            BriefingSourceResDTO.TYPE_NEWS
                    ))
                    .forEach(items::add);
        }

        if (!disclosureIds.isEmpty()) {
            disclosureAnalysisRepository.findWithDisclosureByDisclosureIdIn(disclosureIds).stream()
                    .map(da -> new BriefingSourceResDTO.SourceItem(
                            da.getDisclosure().getId(),
                            da.getDisclosure().getDisclosureType(),
                            null,
                            da.getDisclosure().getUrl(),
                            BriefingSourceResDTO.TYPE_DISCLOSURE
                    ))
                    .forEach(items::add);
        }

        return new BriefingSourceResDTO.SourceList(items);
    }

    private void cacheUntilMidnight(String cacheKey, BriefingResDTO.BriefingDetail dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            long ttlSeconds = Math.max(
                    ChronoUnit.SECONDS.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()),
                    1L
            );
            redisUtil.save(cacheKey, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("[BriefingQueryService] 캐시 저장 실패, 브리핑은 정상 반환 - error={}", e.getMessage());
        }
    }

    private BriefingResDTO.BriefingDetail toDetail(BriefingResult result) {
        return new BriefingResDTO.BriefingDetail(
                result.title(),
                result.summary(),
                result.newsIds(),
                result.disclosureIds()
        );
    }

    private BriefingResDTO.BriefingDetail emptyDetail() {
        return new BriefingResDTO.BriefingDetail(
                "오늘 브리핑을 준비 중입니다.",
                "아직 분석된 항목이 없어요. 잠시 후 다시 확인해주세요.",
                List.of(),
                List.of()
        );
    }
}