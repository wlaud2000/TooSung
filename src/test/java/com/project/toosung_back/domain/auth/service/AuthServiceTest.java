package com.project.toosung_back.domain.auth.service;

import com.project.toosung_back.domain.auth.exception.AuthErrorCode;
import com.project.toosung_back.domain.auth.repository.LocalAuthRepository;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import com.project.toosung_back.global.apiPayload.exception.CustomException;
import com.project.toosung_back.global.security.dto.JwtDTO;
import com.project.toosung_back.global.security.utils.CookieUtil;
import com.project.toosung_back.global.security.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthService 단위 테스트
 *
 * 핵심 개념
 * - @InjectMocks: 테스트 대상 클래스. @Mock들이 자동으로 주입됨
 * - @Mock: 의존성들을 가짜로 대체 (JwtUtil, CookieUtil 등)
 *
 * 이 테스트가 검증하는 것
 * AuthService.reissueToken()의 RTR 오케스트레이션 흐름:
 * 1. 쿠키에서 Refresh Token 추출
 * 2. 토큰 유효성 검증
 * 3. RTR 패턴 적용 (JwtUtil에 위임)
 * 4. 새 토큰을 쿠키에 저장
 *
 * Q. JwtUtil의 reissueToken 로직이 이미 테스트됐는데 여기서 또 테스트하는 이유?
 * A. 이 테스트는 "JwtUtil 내부 로직"이 아닌 "AuthService의 흐름 제어"를 검증한다.
 *    예: 쿠키가 없을 때 예외를 던지는가? SecurityException 발생 시 쿠키를 삭제하는가?
 *    각 테스트는 자신의 책임 범위(단일 책임)만 검증해야 함.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    // @InjectMocks: 실제 테스트 대상 객체 (AuthService 인스턴스 생성 + Mock 주입)
    @InjectMocks
    private AuthService authService;

    // @Mock: 테스트에 필요한 모든 의존성을 가짜로 대체
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private LocalAuthRepository localAuthRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CookieUtil cookieUtil;

    // HTTP 요청/응답도 Mock으로 대체 (실제 HTTP 서버 불필요)
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Nested
    @DisplayName("토큰 재발급 (RTR)")
    class ReissueTokenTest {

        private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";
        private static final String NEW_ACCESS_TOKEN = "new.access.token";
        private static final String NEW_REFRESH_TOKEN = "new.refresh.token";

        @Test
        @DisplayName("정상 재발급: 새 토큰 쌍이 쿠키에 저장된다")
        void reissueToken_validToken_savesNewTokensToCookie() {
            // given
            given(cookieUtil.extractFromCookie(request, "refresh_token"))
                    .willReturn(VALID_REFRESH_TOKEN);
            willDoNothing().given(jwtUtil).validateToken(VALID_REFRESH_TOKEN);

            JwtDTO newTokens = JwtDTO.builder()
                    .accessToken(NEW_ACCESS_TOKEN)
                    .refreshToken(NEW_REFRESH_TOKEN)
                    .build();
            given(jwtUtil.reissueToken(VALID_REFRESH_TOKEN)).willReturn(newTokens);
            given(jwtUtil.getAccessExpMs()).willReturn(1800000L);
            given(jwtUtil.getRefreshExpMs()).willReturn(604800000L);

            // when
            JwtDTO result = authService.reissueToken(request, response);

            // then: 반환값 확인
            assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);

            // 새 토큰이 쿠키에 저장됐는지 확인
            then(cookieUtil).should().addCookie(eq(response), eq("access_token"), eq(NEW_ACCESS_TOKEN), anyLong());
            then(cookieUtil).should().addCookie(eq(response), eq("refresh_token"), eq(NEW_REFRESH_TOKEN), anyLong());
        }

        @Test
        @DisplayName("쿠키에 Refresh Token이 없으면 REFRESH_TOKEN_NOT_FOUND 예외 발생")
        void reissueToken_noRefreshTokenInCookie_throwsCustomException() {
            // given: 쿠키에 토큰 없음
            given(cookieUtil.extractFromCookie(request, "refresh_token"))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
                    });

            // 쿠키 없으면 이후 로직 실행 안됨
            then(jwtUtil).should(never()).validateToken(any());
        }

        @Test
        @DisplayName("만료된 Refresh Token이면 INVALID_REFRESH_TOKEN 예외 발생")
        void reissueToken_expiredToken_throwsCustomException() {
            // given
            given(cookieUtil.extractFromCookie(request, "refresh_token"))
                    .willReturn(VALID_REFRESH_TOKEN);
            // validateToken이 ExpiredJwtException 던지는 상황 시뮬레이션
            willThrow(new ExpiredJwtException(null, null, "만료"))
                    .given(jwtUtil).validateToken(VALID_REFRESH_TOKEN);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("변조된 Refresh Token이면 INVALID_REFRESH_TOKEN 예외 발생")
        void reissueToken_tamperedToken_throwsCustomException() {
            // given
            given(cookieUtil.extractFromCookie(request, "refresh_token"))
                    .willReturn(VALID_REFRESH_TOKEN);
            willThrow(new SecurityException("잘못된 서명"))
                    .given(jwtUtil).validateToken(VALID_REFRESH_TOKEN);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
                    });
        }

        @Test
        @DisplayName("RTR 재사용 감지 시: 쿠키 삭제 + TOKEN_REUSE_DETECTED 예외 발생")
        void reissueToken_reuseDetected_deletesCookiesAndThrowsCustomException() {
            // given
            given(cookieUtil.extractFromCookie(request, "refresh_token"))
                    .willReturn(VALID_REFRESH_TOKEN);
            willDoNothing().given(jwtUtil).validateToken(VALID_REFRESH_TOKEN);
            // JwtUtil.reissueToken이 재사용 감지로 SecurityException 던지는 상황
            willThrow(new SecurityException("토큰 재사용이 감지되었습니다."))
                    .given(jwtUtil).reissueToken(VALID_REFRESH_TOKEN);

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ex = (CustomException) e;
                        assertThat(ex.getCode()).isEqualTo(AuthErrorCode.TOKEN_REUSE_DETECTED);
                    });

            // 공격 감지 시 쿠키도 반드시 삭제해야 함
            then(cookieUtil).should().deleteCookie(response, "access_token");
            then(cookieUtil).should().deleteCookie(response, "refresh_token");
        }
    }
}
