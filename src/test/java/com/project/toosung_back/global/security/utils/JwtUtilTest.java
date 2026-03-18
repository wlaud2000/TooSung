package com.project.toosung_back.global.security.utils;

import com.project.toosung_back.global.security.dto.JwtDTO;
import com.project.toosung_back.global.security.dto.TokenInfo;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import com.project.toosung_back.global.security.userdetails.CustomUserDetails;
import com.project.toosung_back.global.utils.RedisUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * JwtUtil 단위 테스트
 *
 * ✅ 핵심 개념
 * - @ExtendWith(MockitoExtension.class): Spring 컨텍스트 없이 Mockito만 사용 → 빠름
 * - @Mock: RedisUtil을 가짜 객체로 대체 (실제 Redis 연결 없음)
 * - given-when-then: BDD 스타일 테스트 구조
 *
 * Q. 단위 테스트에서 Redis를 직접 쓰지 않는 이유?
 * A. 단위 테스트는 특정 클래스의 로직만 검증해야 한다.
 *    외부 인프라(Redis)에 의존하면 Redis 장애 시 테스트도 실패하므로,
 *    Mock으로 격리하여 JwtUtil 자체 로직만 테스트 함.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil 단위 테스트")
class JwtUtilTest {

    // @Mock: 실제 Redis 없이 동작하는 가짜 RedisUtil
    @Mock
    private RedisUtil redisUtil;

    private JwtUtil jwtUtil;

    // 테스트용 고정값 (운영 환경의 실제 시크릿 키와 분리)
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hs256-algorithm";
    private static final Long ACCESS_EXP_MS = 1000L * 60 * 30;       // 30분
    private static final Long REFRESH_EXP_MS = 1000L * 60 * 60 * 24 * 7; // 7일

    private CustomUserDetails testUserDetails;

    /**
     * 각 테스트 실행 전 공통 사전 설정
     * JwtUtil을 직접 생성 (Spring DI 없이)
     */
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, ACCESS_EXP_MS, REFRESH_EXP_MS, redisUtil);

        // 테스트용 사용자 (Member 엔티티 없이 AuthUser로 직접 생성)
        AuthUser authUser = new AuthUser(1L, "test@example.com", "encoded-password");
        testUserDetails = new CustomUserDetails(authUser);
    }

    // =========================================================
    // 1. 토큰 생성 테스트
    // =========================================================

    @Nested
    @DisplayName("토큰 생성")
    class CreateTokenTest {

        @Test
        @DisplayName("Access Token은 JWT 형식(header.payload.signature)으로 생성된다")
        void createAccessToken_returnsValidJwtFormat() {
            // when
            String token = jwtUtil.createAccessToken(testUserDetails);

            // then
            assertThat(token).isNotNull();
            // JWT는 반드시 '.' 으로 구분된 3파트 구조
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Refresh Token 생성 시 Redis에 'refresh:{email}' 키로 저장된다")
        void createRefreshToken_savesTokenToRedis() {
            // when
            String token = jwtUtil.createRefreshToken(testUserDetails);

            // then: Redis에 올바른 키/값/만료시간으로 저장됐는지 검증
            // ✅ then(mock).should(): Mock 메서드 호출 여부 검증 (행위 검증)
            then(redisUtil).should().save(
                    eq("refresh:" + testUserDetails.getEmail()), // 키
                    eq(token),                                   // 값
                    eq(REFRESH_EXP_MS),                         // 만료시간
                    eq(TimeUnit.MILLISECONDS)                   // 시간 단위
            );
        }
    }

    // =========================================================
    // 2. 토큰 파싱 테스트
    // =========================================================

    @Nested
    @DisplayName("토큰 파싱")
    class ParseTokenTest {

        @Test
        @DisplayName("토큰에서 이메일(subject)을 정확히 추출한다")
        void getEmail_extractsEmailFromToken() {
            // given
            String token = jwtUtil.createAccessToken(testUserDetails);

            // when
            String email = jwtUtil.getEmail(token);

            // then
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("토큰에서 userId(custom claim)를 정확히 추출한다")
        void getUserId_extractsUserIdFromToken() {
            // given
            String token = jwtUtil.createAccessToken(testUserDetails);

            // when
            Long userId = jwtUtil.getUserId(token);

            // then
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("TokenInfo 추출 시 email과 memberId를 모두 반환한다")
        void extractTokenInfo_returnsBothEmailAndMemberId() {
            // given
            String token = jwtUtil.createAccessToken(testUserDetails);

            // when
            TokenInfo tokenInfo = jwtUtil.extractTokenInfo(token);

            // then
            assertThat(tokenInfo.email()).isEqualTo("test@example.com");
            assertThat(tokenInfo.memberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("유효한 토큰의 남은 만료시간은 0보다 크다")
        void getRemainingTime_validToken_returnsPositiveValue() {
            // given
            String token = jwtUtil.createAccessToken(testUserDetails);

            // when
            long remainingTime = jwtUtil.getRemainingTime(token);

            // then
            assertThat(remainingTime).isGreaterThan(0);
        }

        @Test
        @DisplayName("만료된 토큰의 남은 만료시간은 0이다")
        void getRemainingTime_expiredToken_returnsZero() {
            // given: 과거 시점으로 만료된 토큰 생성 (clockSkew 허용 범위 초과)
            String expiredToken = jwtUtil.createToken(testUserDetails, Instant.now().minusSeconds(400));

            // when
            long remainingTime = jwtUtil.getRemainingTime(expiredToken);

            // then
            assertThat(remainingTime).isEqualTo(0);
        }
    }

    // =========================================================
    // 3. 토큰 검증 테스트
    // =========================================================

    @Nested
    @DisplayName("토큰 검증")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰은 예외 없이 검증을 통과한다")
        void validateToken_validToken_noException() {
            // given
            String token = jwtUtil.createAccessToken(testUserDetails);
            given(redisUtil.hasKey("blacklist:" + token)).willReturn(false);

            // when & then: 예외가 발생하지 않아야 한다
            assertThatNoException().isThrownBy(() -> jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("블랙리스트에 등록된 토큰은 SecurityException 발생 (로그아웃된 토큰)")
        void validateToken_blacklistedToken_throwsSecurityException() {
            // given: 블랙리스트에 등록된 상태 시뮬레이션
            String token = jwtUtil.createAccessToken(testUserDetails);
            given(redisUtil.hasKey("blacklist:" + token)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> jwtUtil.validateToken(token))
                    .isInstanceOf(SecurityException.class)
                    .hasMessage("무효화된 토큰입니다.");
        }

        @Test
        @DisplayName("서명이 변조된 토큰은 SecurityException 발생")
        void validateToken_tamperedSignature_throwsSecurityException() {
            // given: 토큰 뒤에 임의 문자열 추가 → 서명 불일치
            String token = jwtUtil.createAccessToken(testUserDetails);
            String tamperedToken = token + "tampered";
            given(redisUtil.hasKey("blacklist:" + tamperedToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> jwtUtil.validateToken(tamperedToken))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("만료된 토큰은 ExpiredJwtException 발생")
        void validateToken_expiredToken_throwsExpiredJwtException() {
            // given: clockSkewSeconds(180) 허용 범위(3분)를 초과하여 만료된 토큰 생성
            // createToken()은 public이므로 직접 과거 만료시간 지정 가능
            String expiredToken = jwtUtil.createToken(testUserDetails, Instant.now().minusSeconds(400));
            given(redisUtil.hasKey("blacklist:" + expiredToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> jwtUtil.validateToken(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("완전히 잘못된 형식의 토큰은 SecurityException 발생")
        void validateToken_malformedToken_throwsSecurityException() {
            // given
            String malformedToken = "this.is.not.a.valid.jwt";
            given(redisUtil.hasKey("blacklist:" + malformedToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> jwtUtil.validateToken(malformedToken))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // =========================================================
    // 4. RTR(Refresh Token Rotation) 핵심 로직 테스트
    //
    // Q. RTR 전략이란?
    // A. Refresh Token을 한 번 사용하면 즉시 새 토큰으로 교체하는 전략.
    //    만약 이미 사용된 토큰(Redis에 없는 토큰)으로 재발급을 시도하면
    //    누군가 토큰을 탈취한 것으로 판단하고 모든 세션을 무효화 함.
    // =========================================================

    @Nested
    @DisplayName("RTR 토큰 재발급")
    class ReissueTokenTest {

        @Test
        @DisplayName("정상 재발급: 기존 토큰 삭제 후 새 Access/Refresh 토큰 발급")
        void reissueToken_validToken_issuesNewTokenPair() {
            // given
            String refreshToken = jwtUtil.createRefreshToken(testUserDetails);
            // Redis에 해당 토큰이 저장되어 있는 정상 상태
            given(redisUtil.get("refresh:" + testUserDetails.getEmail()))
                    .willReturn(refreshToken);

            // when
            JwtDTO newTokens = jwtUtil.reissueToken(refreshToken);

            // then: 새 토큰 쌍 발급 확인
            assertThat(newTokens.accessToken()).isNotNull();
            assertThat(newTokens.refreshToken()).isNotNull();

            // 기존 토큰 삭제 확인 (RTR의 핵심: 토큰 재사용 방지)
            then(redisUtil).should().delete("refresh:" + testUserDetails.getEmail());
        }

        @Test
        @DisplayName("재사용 감지 - Redis에 토큰이 없을 때 (이미 재발급된 토큰으로 재시도)")
        void reissueToken_tokenNotInRedis_throwsSecurityException() {
            // given
            String refreshToken = jwtUtil.createRefreshToken(testUserDetails);
            // 시나리오: 이 토큰은 이미 한 번 사용되어 Redis에서 삭제된 상태
            given(redisUtil.get("refresh:" + testUserDetails.getEmail()))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> jwtUtil.reissueToken(refreshToken))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("토큰 재사용이 감지");

            // 재사용 감지 시 Redis에서 관련 토큰 전부 삭제 (세션 무효화)
            then(redisUtil).should().delete("refresh:" + testUserDetails.getEmail());
        }

        @Test
        @DisplayName("재사용 감지 - 공격 시나리오: 탈취된 구 토큰으로 재발급 시도")
        void reissueToken_stolenOldToken_throwsSecurityException() {
            // given
            // 공격 시나리오:
            // 1. 사용자가 tokenA로 정상 재발급 → tokenB 발급됨, Redis에 tokenB 저장
            // 2. 공격자가 훔쳐둔 tokenA로 재발급 시도 → Redis에는 tokenB가 있으므로 불일치

            String stolenOldToken = jwtUtil.createRefreshToken(testUserDetails); // 공격자가 훔친 구 토큰

            // Redis에는 이미 재발급된 최신 토큰(다른 값)이 저장되어 있음을 명시적으로 Mock
            // (같은 밀리초에 두 토큰을 생성하면 동일 토큰이 될 수 있어 Mock으로 명확히 구분)
            given(redisUtil.get("refresh:" + testUserDetails.getEmail()))
                    .willReturn("latest.token.already.issued.after.rotation");

            // when & then: 구 토큰으로 재발급 시도 → 재사용 감지
            assertThatThrownBy(() -> jwtUtil.reissueToken(stolenOldToken))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("토큰 재사용이 감지");

            // 공격 감지 시 세션 전체 무효화
            then(redisUtil).should().delete("refresh:" + testUserDetails.getEmail());
        }
    }

    // =========================================================
    // 5. 블랙리스트 등록 테스트 (로그아웃)
    // =========================================================

    @Nested
    @DisplayName("블랙리스트 등록")
    class BlacklistTest {

        @Test
        @DisplayName("잔여 만료시간이 있는 토큰은 블랙리스트에 등록된다")
        void addToBlacklist_withRemainingTime_registeredToBlacklist() {
            // given
            String token = "some.jwt.token";
            long remainingSeconds = 1800L; // 30분 남음

            // when
            jwtUtil.addToBlacklist(token, remainingSeconds);

            // then: 블랙리스트 키로 저장됐는지 확인
            then(redisUtil).should().save(
                    eq("blacklist:" + token),
                    eq("logout"),
                    eq(remainingSeconds),
                    eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("잔여 만료시간이 0인 토큰은 블랙리스트에 등록하지 않는다 (이미 만료됨)")
        void addToBlacklist_zeroRemainingTime_notRegistered() {
            // given
            String token = "some.jwt.token";

            // when
            jwtUtil.addToBlacklist(token, 0L);

            // then: Redis 저장 호출 없어야 함
            then(redisUtil).should(never()).save(any(), any(), anyLong(), any());
        }
    }
}
