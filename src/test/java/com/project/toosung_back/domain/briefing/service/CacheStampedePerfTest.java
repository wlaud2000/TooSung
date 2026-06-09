package com.project.toosung_back.domain.briefing.service;

import com.project.toosung_back.domain.briefing.ai.dto.BriefingResult;
import com.project.toosung_back.domain.briefing.service.query.BriefingQueryService;
import com.project.toosung_back.global.utils.RedisUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class CacheStampedePerfTest {

    @Autowired private BriefingQueryService briefingQueryService;
    @Autowired private RedisUtil redisUtil;

    @MockitoBean private BriefingGeneratorService briefingGeneratorService;

    private static final Long MEMBER_ID = 999L;
    private static final String CACHE_KEY = "briefing:" + MEMBER_ID;

    @BeforeEach
    void setUp() throws Exception {
        redisUtil.delete(CACHE_KEY);

        given(briefingGeneratorService.generate(MEMBER_ID))
                .willAnswer(inv -> {
                    Thread.sleep(200); // AI 호출 시뮬레이션
                    return new BriefingResult("오늘의 브리핑", "요약 내용", List.of(), List.of());
                });
    }

    @AfterEach
    void tearDown() {
        redisUtil.delete(CACHE_KEY);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Before: 락 없는 구 동작 재현 — 캐시 미스 시 모든 스레드가 AI 직접 호출
    //   실제 서비스는 이미 락이 적용됐으므로, 구 패턴을 테스트 내부에서 직접 시뮬레이션
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("[Before] 동시 10개 요청 — 락 없음, AI 중복 호출 (구 동작 재현)")
    void before_캐시_스탬피드() throws InterruptedException {
        StampedResult result = runConcurrentNoLock(10, false);
        printResult("[Before] 플랫폼 스레드 10개 — 락 없음", result);
    }

    @Test
    @DisplayName("[Before] 동시 10,000개 요청 — Virtual Thread, 락 없음")
    void before_대용량_스탬피드() throws InterruptedException {
        StampedResult result = runConcurrentNoLock(10_000, true);
        printResult("[Before] Virtual Thread 10,000개 — 락 없음", result);
    }

    // ─────────────────────────────────────────────────────────────────────
    // After: Redis 분산 락 적용 — 1개 스레드만 AI 호출, 나머지는 캐시 대기
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("[After] 동시 10개 요청 — 분산 락, AI 1회만 호출")
    void after_분산_락() throws InterruptedException {
        StampedResult result = runConcurrentWithLock(10, false);
        printResult("[After] 플랫폼 스레드 10개 — 분산 락", result);
    }

    @Test
    @DisplayName("[After] 동시 10,000개 요청 — Virtual Thread, 분산 락")
    void after_대용량_분산_락() throws InterruptedException {
        StampedResult result = runConcurrentWithLock(10_000, true);
        printResult("[After] Virtual Thread 10,000개 — 분산 락", result);
    }

    // ── 구 동작 재현: 캐시 미스 시 락 없이 바로 generate() 호출 ─────────
    private StampedResult runConcurrentNoLock(int threadCount, boolean virtual) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        Thread.Builder builder = virtual ? Thread.ofVirtual() : Thread.ofPlatform();

        IntStream.range(0, threadCount).forEach(i ->
                builder.start(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        // 구 패턴: 캐시 확인 → 미스 → 락 없이 generate() 바로 호출
                        if (!redisUtil.hasKey(CACHE_KEY)) {
                            briefingGeneratorService.generate(MEMBER_ID);
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                })
        );

        ready.await();
        StopWatch sw = new StopWatch();
        sw.start();
        start.countDown();
        done.await();
        sw.stop();

        long callCount = Mockito.mockingDetails(briefingGeneratorService)
                .getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("generate"))
                .count();

        return new StampedResult(threadCount, sw.getTotalTimeMillis(), callCount, errorCount.get(), virtual);
    }

    // ── 신 동작: BriefingQueryService (분산 락 적용) ─────────────────────
    private StampedResult runConcurrentWithLock(int threadCount, boolean virtual) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        Thread.Builder builder = virtual ? Thread.ofVirtual() : Thread.ofPlatform();

        IntStream.range(0, threadCount).forEach(i ->
                builder.start(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        briefingQueryService.getTodayBriefing(MEMBER_ID);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                })
        );

        ready.await();
        StopWatch sw = new StopWatch();
        sw.start();
        start.countDown();
        done.await();
        sw.stop();

        long callCount = Mockito.mockingDetails(briefingGeneratorService)
                .getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("generate"))
                .count();

        return new StampedResult(threadCount, sw.getTotalTimeMillis(), callCount, errorCount.get(), virtual);
    }

    private void printResult(String label, StampedResult r) {
        System.out.println();
        System.out.println("===== " + label + " =====");
        System.out.printf("스레드 종류  : %s%n", r.virtual ? "Virtual Thread" : "Platform Thread");
        System.out.printf("동시 요청 수 : %,d 개%n", r.threadCount);
        System.out.printf("총 실행 시간 : %d ms%n", r.elapsedMs);
        System.out.printf("AI 호출 횟수 : %,d 회%n", r.aiCallCount);
        System.out.printf("에러 발생   : %d 건%n", r.errorCount);
        System.out.println("=".repeat(label.length() + 13));
    }

    private record StampedResult(int threadCount, long elapsedMs, long aiCallCount, int errorCount, boolean virtual) {}
}
