package com.project.toosung_back.domain.auth.service.oauth.strategy;

import com.project.toosung_back.domain.auth.dto.response.OAuthUserInfo;
import com.project.toosung_back.domain.auth.enums.Provider;

/**
 * OAuth Strategy 인터페이스
 * 각 소셜 로그인 Provider(카카오, 구글 등)는 이 인터페이스를 구현
 */
public interface OAuthStrategy {

    /**
     * 지원하는 Provider 타입 반환
     */
    Provider getProvider();

    /**
     * OAuth 인증 URL 생성
     * @param state CSRF 방지용 state 값
     * @return 소셜 로그인 페이지 URL
     */
    String getAuthorizationUrl(String state);

    /**
     * 인가 코드로 액세스 토큰 발급
     * @param code 인가 코드
     * @return 액세스 토큰
     */
    String getAccessToken(String code);

    /**
     * 액세스 토큰으로 사용자 정보 조회
     * @param accessToken 액세스 토큰
     * @return 통합 사용자 정보
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
