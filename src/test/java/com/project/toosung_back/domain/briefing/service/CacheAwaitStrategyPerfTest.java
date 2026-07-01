package com.project.toosung_back.domain.briefing.service;

import com.project.toosung_back.global.utils.RedisUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
@ActiveProfiles("test")
class CacheAwaitStrategyPerfTest {

    @Autowired private RedisUtil redisUtil;
    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private RedisMessageListenerContainer listenerContainer;

    private static final long AI_SIMULATION_MS  = 200;
    private static final long POLL_INTERVAL_MS  = 200;
    private static final int  MAX_POLL_ATTEMPTS = 15;

    private String testKey;

    @AfterEach
    void tearDown() {
        if (testKey != null) redisUtil.delete(testKey);
    }

    @Test
    @DisplayName("[Before] 폴링 방식 — 플랫폼 스레드 10개 알림 지연")
    void before_polling_10_threads() throws InterruptedException {
        runPollingScenario(10, false, "[Before] 폴링 — 플랫폼 스레드 10개");
    }

    @Test
    @DisplayName("[Before] 폴링 방식 — Virtual Thread 1,000개 알림 지연")
    void before_polling_1000_virtual() throws InterruptedException {
        runPollingScenario(1_000, true, "[Before] 폴링 — Virtual Thread 1,000개");
    }

    @Test
    @DisplayName("[After] Pub/Sub 방식 — 플랫폼 스레드 10개 알림 지연")
    void after_pubsub_10_threads() throws InterruptedException {
        runPubSubScenario(10, false, "[After] Pub/Sub — 플랫폼 스레드 10개");
    }

    @Test
    @DisplayName("[After] Pub/Sub 방식 — Virtual Thread 1,000개 알림 지연")
    void after_pubsub_1000_virtual() throws InterruptedException {
        runPubSubScenario(1_000, true, "[After] Pub/Sub — Virtual Thread 1,000개");
    }

    private void runPollingScenario(int readerCount, boolean virtual, String label) throws InterruptedException {
        testKey = "perf:poll:" + UUID.randomUUID();
        final String key = testKey;

        AtomicLong cacheWrittenAt    = new AtomicLong(-1);
        AtomicInteger totalRedisReads = new AtomicInteger(0);
        List<Long> lags = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch readersReady = new CountDownLatch(readerCount);
        CountDownLatch start        = new CountDownLatch(1);
        CountDownLatch done         = new CountDownLatch(readerCount);

        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(AI_SIMULATION_MS);
                redisUtil.save(key, "\"result\"", 60L, TimeUnit.SECONDS);
                cacheWrittenAt.set(System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.Builder builder = virtual ? Thread.ofVirtual() : Thread.ofPlatform();
        for (int i = 0; i < readerCount; i++) {
            builder.start(() -> {
                readersReady.countDown();
                try {
                    start.await();
                    for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
                        Thread.sleep(POLL_INTERVAL_MS);
                        totalRedisReads.incrementAndGet();
                        String val = redisUtil.get(key);
                        if (val != null) {
                            long written = cacheWrittenAt.get();
                            if (written > 0) lags.add(System.currentTimeMillis() - written);
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        readersReady.await();
        StopWatch sw = new StopWatch();
        sw.start();
        start.countDown();
        done.await();
        sw.stop();

        printResult(label, readerCount, sw.getTotalTimeMillis(), lags, totalRedisReads.get(), virtual);
    }

    private void runPubSubScenario(int readerCount, boolean virtual, String label) throws InterruptedException {
        testKey = "perf:pubsub:" + UUID.randomUUID();
        final String key     = testKey;
        final String channel = "perf:ready:" + UUID.randomUUID();

        AtomicLong cacheWrittenAt = new AtomicLong(-1);
        List<Long> lags = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch readersReady = new CountDownLatch(readerCount);
        CountDownLatch start        = new CountDownLatch(1);
        CountDownLatch done         = new CountDownLatch(readerCount);

        ChannelTopic topic = new ChannelTopic(channel);

        // 리스너 1개 — 1회 publish로 N개 스레드 언블로킹 (Pub/Sub 핵심)
        CompletableFuture<Long> notifyFuture = new CompletableFuture<>();
        MessageListener sharedListener = (message, pattern) -> {
            long written = cacheWrittenAt.get();
            notifyFuture.complete(written > 0 ? System.currentTimeMillis() - written : 0L);
        };
        listenerContainer.addMessageListener(sharedListener, topic);

        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(AI_SIMULATION_MS);
                redisUtil.save(key, "\"result\"", 60L, TimeUnit.SECONDS);
                cacheWrittenAt.set(System.currentTimeMillis());
                stringRedisTemplate.convertAndSend(channel, "1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.Builder builder = virtual ? Thread.ofVirtual() : Thread.ofPlatform();
        for (int i = 0; i < readerCount; i++) {
            builder.start(() -> {
                readersReady.countDown();
                try {
                    start.await();
                    // race condition 방어: 이미 캐시가 있으면 즉시 complete
                    if (redisUtil.hasKey(key)) {
                        notifyFuture.complete(0L);
                    }
                    Long lag = notifyFuture.get(30, TimeUnit.SECONDS);
                    lags.add(lag);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // timeout
                } finally {
                    done.countDown();
                }
            });
        }

        readersReady.await();
        StopWatch sw = new StopWatch();
        sw.start();
        start.countDown();
        done.await();
        sw.stop();
        listenerContainer.removeMessageListener(sharedListener, topic);

        System.out.println();
        System.out.println("===== " + label + " =====");
        System.out.printf("스레드 종류    : %s%n", virtual ? "Virtual Thread" : "Platform Thread");
        System.out.printf("동시 대기 수   : %,d 개%n", readerCount);
        System.out.printf("총 실행 시간   : %d ms%n", sw.getTotalTimeMillis());
        System.out.printf("Redis 알림 발행 : 1 회 → %,d 스레드 언블로킹%n", readerCount);
        long avgLag = lags.isEmpty() ? -1 : lags.stream().mapToLong(Long::longValue).sum() / lags.size();
        long maxLag = lags.stream().mapToLong(Long::longValue).max().orElse(-1);
        System.out.printf("평균 알림 지연  : %d ms%n", avgLag);
        System.out.printf("최대 알림 지연  : %d ms%n", maxLag);
        System.out.println("=".repeat(label.length() + 13));
    }

    private void printResult(String label, int threadCount, long totalMs, List<Long> lags, int callCount, boolean virtual) {
        long avgLag = lags.isEmpty() ? -1 : lags.stream().mapToLong(Long::longValue).sum() / lags.size();
        long maxLag = lags.stream().mapToLong(Long::longValue).max().orElse(-1);

        System.out.println();
        System.out.println("===== " + label + " =====");
        System.out.printf("스레드 종류    : %s%n", virtual ? "Virtual Thread" : "Platform Thread");
        System.out.printf("동시 대기 수   : %,d 개%n", threadCount);
        System.out.printf("총 실행 시간   : %d ms%n", totalMs);
        System.out.printf("Redis 호출 수  : %,d 회%n", callCount);
        System.out.printf("평균 알림 지연  : %d ms%n", avgLag);
        System.out.printf("최대 알림 지연  : %d ms%n", maxLag);
        System.out.println("=".repeat(label.length() + 13));
    }
}
