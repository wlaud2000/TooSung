package com.project.toosung_back.global.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictNewsCache(Long stockId) {
        deleteByPattern("news:list:" + stockId + ":*");
    }

    public void evictDisclosureCache(Long stockId) {
        deleteByPattern("disclosure:list:" + stockId + ":*");
    }

    public void evictBriefingCache(Long memberId) {
        deleteByPattern("briefing:" + memberId);
    }

    public void evictSectorAnalysisCache(Long memberId) {
        deleteByPattern("sector-analysis:" + memberId);
    }

    public void evictAllBriefingCaches() {
        deleteByPattern("briefing:*");
    }

    public void evictAllSectorAnalysisCaches() {
        deleteByPattern("sector-analysis:*");
    }

    private void deleteByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            log.warn("[CacheEvict] SCAN 실패: pattern={}", pattern, e);
            return;
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("[CacheEvict] 캐시 삭제 완료: pattern={}, count={}", pattern, keys.size());
        }
    }
}
