package com.project.toosung_back.global.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictNewsCache(Long stockId) {
        deleteByPattern("news:list:" + stockId + ":*");
    }

    public void evictDisclosureCache(Long stockId) {
        deleteByPattern("disclosure::" + stockId + ":*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("[CacheEvict] 캐시 삭제 완료: pattern={}, count={}", pattern, keys.size());
        }
    }
}
