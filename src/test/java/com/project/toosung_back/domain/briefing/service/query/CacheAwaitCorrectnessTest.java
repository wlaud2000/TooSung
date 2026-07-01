package com.project.toosung_back.domain.briefing.service.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.briefing.dto.response.BriefingResDTO;
import com.project.toosung_back.global.utils.RedisUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "cache.lock.ttl=1")
class CacheAwaitCorrectnessTest {

    @Autowired private BriefingQueryService briefingQueryService;
    @Autowired private RedisUtil redisUtil;
    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private ObjectMapper objectMapper;
    @MockitoSpyBean private RedisMessageListenerContainer listenerContainer;

    private static final Long   MEMBER_ID = 997L;
    private static final String CACHE_KEY = "briefing:" + MEMBER_ID;
    private static final String CHANNEL   = "briefing:ready:" + MEMBER_ID;

    @BeforeEach
    void setUp() {
        redisUtil.delete(CACHE_KEY);
    }

    @AfterEach
    void tearDown() {
        redisUtil.delete(CACHE_KEY);
    }

    @Test
    @DisplayName("awaitCache() 완료 후 리스너가 컨테이너에서 제거된다")
    void awaitCache_완료_후_리스너가_제거된다() throws InterruptedException {
        // publish만 발행 (캐시 없음) → 리스너: readCache() null → emptyDetail로 future 완료
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(100);
                stringRedisTemplate.convertAndSend(CHANNEL, "1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        briefingQueryService.awaitCache(CACHE_KEY, MEMBER_ID);

        // finally 블록의 removeMessageListener 호출 검증
        verify(listenerContainer, atLeastOnce())
                .removeMessageListener(any(MessageListener.class), any(ChannelTopic.class));
    }

    @Test
    @DisplayName("subscribe 전에 publish가 먼저 온 경우 double-check로 즉시 반환된다")
    void subscribe_전에_publish가_먼저_온_경우_double_check로_처리() throws Exception {
        // publish가 이미 왔고 아무도 못 받은 상황 재현: 캐시만 저장, publish 없음
        BriefingResDTO.BriefingDetail expected = new BriefingResDTO.BriefingDetail(
                "double-check 브리핑", "요약", List.of(), List.of()
        );
        redisUtil.save(CACHE_KEY, objectMapper.writeValueAsString(expected), 60L, TimeUnit.SECONDS);

        // awaitCache(): subscribe → double-check에서 캐시 발견 → publish 없이 즉시 반환
        long start = System.currentTimeMillis();
        BriefingResDTO.BriefingDetail result = briefingQueryService.awaitCache(CACHE_KEY, MEMBER_ID);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.title()).isEqualTo("double-check 브리핑");
        assertThat(elapsed).isLessThan(200); // 1초 타임아웃 대기 없이 즉시 반환
    }

    @Test
    @DisplayName("publish가 없으면 lockTtlSeconds(1초) 후 emptyDetail을 반환한다")
    void publish가_없으면_타임아웃_후_emptyDetail_반환() {
        // publish 없이 awaitCache() 호출 → future.get(1, SECONDS) → TimeoutException → emptyDetail
        long start = System.currentTimeMillis();
        BriefingResDTO.BriefingDetail result = briefingQueryService.awaitCache(CACHE_KEY, MEMBER_ID);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.title()).isEqualTo("오늘 브리핑을 준비 중입니다.");
        assertThat(elapsed).isGreaterThanOrEqualTo(1_000).isLessThan(5_000);
    }
}
