package com.project.toosung_back.domain.briefing.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.briefing.ai.dto.BriefingResult;
import com.project.toosung_back.domain.briefing.dto.response.BriefingResDTO;
import com.project.toosung_back.domain.briefing.service.BriefingGeneratorService;
import com.project.toosung_back.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
// 오늘의 브리핑을 Redis 캐시 우선으로 조회하고, 없으면 생성해서 반환하는 서비스
public class BriefingQueryService {

    private static final String CACHE_KEY_PREFIX = "briefing:";

    private final BriefingGeneratorService briefingGeneratorService;
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

    // TTL을 자정까지로 계산
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
                result.newsIds()
        );
    }

    private BriefingResDTO.BriefingDetail emptyDetail() {
        return new BriefingResDTO.BriefingDetail(
                "오늘 브리핑을 준비 중입니다.",
                "아직 분석된 항목이 없어요. 잠시 후 다시 확인해주세요.",
                List.of()
        );
    }
}