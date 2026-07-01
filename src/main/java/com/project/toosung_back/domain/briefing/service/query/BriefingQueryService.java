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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingQueryService {

    private static final String CACHE_KEY_PREFIX = "briefing:";
    private static final String LOCK_KEY_PREFIX  = "briefing:lock:";
    private static final String CHANNEL_PREFIX   = "briefing:ready:";

    @Value("${cache.lock.ttl:30}")
    private int lockTtlSeconds;

    private final BriefingGeneratorService briefingGeneratorService;
    private final NewsAnalysisRepository newsAnalysisRepository;
    private final DisclosureAnalysisRepository disclosureAnalysisRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisMessageListenerContainer listenerContainer;

    public BriefingResDTO.BriefingDetail getTodayBriefing(Long memberId) {
        String cacheKey = CACHE_KEY_PREFIX + memberId;

        // 캐시 히트 fast-path
        BriefingResDTO.BriefingDetail cached = readCache(cacheKey);
        if (cached != null) return cached;

        String lockKey = LOCK_KEY_PREFIX + memberId;
        if (redisUtil.setIfAbsent(lockKey, "1", lockTtlSeconds, TimeUnit.SECONDS)) {
            try {
                // 락 획득 후 double-check
                BriefingResDTO.BriefingDetail rechecked = readCache(cacheKey);
                if (rechecked != null) return rechecked;

                BriefingResult result = briefingGeneratorService.generate(memberId);
                BriefingResDTO.BriefingDetail dto = toDetail(result);
                cacheUntilMidnight(cacheKey, dto);
                return dto;
            } catch (Exception e) {
                log.error("[BriefingQueryService] 브리핑 생성 실패 - memberId={}, error={}", memberId, e.getMessage());
                return emptyDetail();
            } finally {
                redisUtil.delete(lockKey);
                stringRedisTemplate.convertAndSend(CHANNEL_PREFIX + memberId, "1");
            }
        }

        return awaitCache(cacheKey, memberId);
    }

    private BriefingResDTO.BriefingDetail readCache(String cacheKey) {
        String raw = redisUtil.get(cacheKey);
        if (raw == null) return null;
        try {
            return objectMapper.readValue(raw, BriefingResDTO.BriefingDetail.class);
        } catch (JsonProcessingException e) {
            log.warn("[BriefingQueryService] 캐시 역직렬화 실패, 삭제 후 재생성");
            redisUtil.delete(cacheKey);
            return null;
        }
    }

    BriefingResDTO.BriefingDetail awaitCache(String cacheKey, Long memberId) {
        String channel = CHANNEL_PREFIX + memberId;
        ChannelTopic topic = new ChannelTopic(channel);
        CompletableFuture<BriefingResDTO.BriefingDetail> future = new CompletableFuture<>();

        MessageListener listener = (message, pattern) -> {
            BriefingResDTO.BriefingDetail result = readCache(cacheKey);
            future.complete(result != null ? result : emptyDetail());
        };

        listenerContainer.addMessageListener(listener, topic);
        try {
            // publish가 subscribe 이전에 발행된 경우 방어
            BriefingResDTO.BriefingDetail existing = readCache(cacheKey);
            if (existing != null) return existing;

            return future.get(lockTtlSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("[BriefingQueryService] Pub/Sub 대기 타임아웃 - memberId={}", memberId);
            return emptyDetail();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return emptyDetail();
        } catch (ExecutionException e) {
            log.warn("[BriefingQueryService] Pub/Sub 리스너 오류 - memberId={}", memberId);
            return emptyDetail();
        } finally {
            listenerContainer.removeMessageListener(listener, topic);
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
            long baseTtl = ChronoUnit.SECONDS.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay());
            long jitter   = ThreadLocalRandom.current().nextLong(-300, 300); // ±5분 랜덤 분산
            long ttl      = Math.max(baseTtl + jitter, 1L);
            redisUtil.save(cacheKey, json, ttl, TimeUnit.SECONDS);
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