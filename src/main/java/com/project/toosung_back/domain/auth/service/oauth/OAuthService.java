package com.project.toosung_back.domain.auth.service.oauth;

import com.project.toosung_back.domain.auth.converter.AuthConverter;
import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.dto.response.OAuthUserInfo;
import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.domain.auth.exception.AuthErrorCode;
import com.project.toosung_back.domain.auth.exception.AuthException;
import com.project.toosung_back.domain.auth.repository.SocialAuthRepository;
import com.project.toosung_back.domain.auth.service.oauth.strategy.OAuthStrategy;
import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import com.project.toosung_back.global.security.userdetails.CustomUserDetails;
import com.project.toosung_back.global.security.utils.CookieUtil;
import com.project.toosung_back.global.security.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class OAuthService {

    private static final String OAUTH_STATE_SESSION_KEY = "oauth_state";

    private final MemberRepository memberRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final Map<Provider, OAuthStrategy> oAuthStrategies;

    public OAuthService(
            MemberRepository memberRepository,
            SocialAuthRepository socialAuthRepository,
            JwtUtil jwtUtil,
            CookieUtil cookieUtil,
            List<OAuthStrategy> strategies
    ) {
        this.memberRepository = memberRepository;
        this.socialAuthRepository = socialAuthRepository;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.oAuthStrategies = strategies.stream()
                .collect(Collectors.toMap(OAuthStrategy::getProvider, Function.identity()));
    }

    /**
     * Provider별 로그인 페이지로 리다이렉트
     */
    public void redirectToProvider(Provider provider, HttpServletResponse response, HttpSession session) throws IOException {
        OAuthStrategy strategy = getStrategy(provider);

        // CSRF 방지용 state 생성 및 세션 저장
        String state = UUID.randomUUID().toString();
        session.setAttribute(OAUTH_STATE_SESSION_KEY, state);

        String authorizationUrl = strategy.getAuthorizationUrl(state);
        response.sendRedirect(authorizationUrl);
    }

    /**
     * OAuth 콜백 처리
     */
    public OAuthResDTO.LoginResponse handleCallback(
            Provider provider,
            String code,
            String state,
            HttpSession session,
            HttpServletResponse response
    ) {
        // 1. state 검증
        validateState(state, session);

        // 2. Strategy 조회
        OAuthStrategy strategy = getStrategy(provider);

        // 3. 액세스 토큰 발급
        String accessToken = strategy.getAccessToken(code);

        // 4. 사용자 정보 조회
        OAuthUserInfo userInfo = strategy.getUserInfo(accessToken);

        // 5. 회원 조회 또는 생성
        Member member = findOrCreateMember(userInfo);

        // 6. JWT 토큰 발급 및 쿠키 저장
        return createLoginResponse(member, response);
    }

    private OAuthStrategy getStrategy(Provider provider) {
        OAuthStrategy strategy = oAuthStrategies.get(provider);
        if (strategy == null) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
        return strategy;
    }

    private void validateState(String state, HttpSession session) {
        String savedState = (String) session.getAttribute(OAUTH_STATE_SESSION_KEY);
        session.removeAttribute(OAUTH_STATE_SESSION_KEY);

        if (savedState == null || !savedState.equals(state)) {
            throw new AuthException(AuthErrorCode.INVALID_STATE);
        }
    }

    private Member findOrCreateMember(OAuthUserInfo userInfo) {
        return socialAuthRepository.findMemberByProviderAndProviderId(userInfo.provider(), userInfo.providerId())
                .orElseGet(() -> createMember(userInfo));
    }

    private Member createMember(OAuthUserInfo userInfo) {
        Member member = memberRepository.save(AuthConverter.toMember(userInfo));
        socialAuthRepository.save(AuthConverter.toSocialAuth(userInfo, member));
        return member;
    }

    private OAuthResDTO.LoginResponse createLoginResponse(Member member, HttpServletResponse response) {
        CustomUserDetails userDetails = new CustomUserDetails(member);
        String accessToken = jwtUtil.createAccessToken(userDetails);
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        // 쿠키에 토큰 저장
        cookieUtil.addCookie(response, "access_token", accessToken, jwtUtil.getAccessExpMs());
        cookieUtil.addCookie(response, "refresh_token", refreshToken, jwtUtil.getRefreshExpMs());

        return AuthConverter.toLoginResponse(member, accessToken, refreshToken);
    }
}
