package com.project.toosung_back.domain.auth.service.oauth.strategy;

import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.dto.response.OAuthUserInfo;
import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.domain.auth.exception.AuthErrorCode;
import com.project.toosung_back.domain.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthStrategy implements OAuthStrategy {

    private static final String AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${spring.oauth.kakao.client-id}")
    private String clientId;

    @Value("${spring.oauth.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.oauth.kakao.redirect-uri}")
    private String redirectUri;

    private final WebClient webClient;

    @Override
    public Provider getProvider() {
        return Provider.kakao;
    }

    @Override
    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUriString(AUTHORIZATION_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public String getAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            OAuthResDTO.KakaoTokenResponse tokenResponse = webClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(params)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("카카오 토큰 요청 실패: {}", body);
                                        return Mono.error(new AuthException(AuthErrorCode.TOKEN_REQUEST_FAILED));
                                    })
                    )
                    .bodyToMono(OAuthResDTO.KakaoTokenResponse.class)
                    .block();

            return tokenResponse != null ? tokenResponse.accessToken() : null;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 토큰 요청 중 오류", e);
            throw new AuthException(AuthErrorCode.TOKEN_REQUEST_FAILED);
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            OAuthResDTO.KakaoUserInfo kakaoUserInfo = webClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("카카오 사용자 정보 요청 실패: {}", body);
                                        return Mono.error(new AuthException(AuthErrorCode.USER_INFO_REQUEST_FAILED));
                                    })
                    )
                    .bodyToMono(OAuthResDTO.KakaoUserInfo.class)
                    .block();

            if (kakaoUserInfo == null) {
                throw new AuthException(AuthErrorCode.USER_INFO_REQUEST_FAILED);
            }

            return OAuthUserInfo.builder()
                    .provider(Provider.kakao)
                    .providerId(String.valueOf(kakaoUserInfo.id()))
                    .email(kakaoUserInfo.getEmail())
                    .nickname(kakaoUserInfo.getNickname())
                    .profileImageUrl(kakaoUserInfo.getProfileImageUrl())
                    .build();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 중 오류", e);
            throw new AuthException(AuthErrorCode.USER_INFO_REQUEST_FAILED);
        }
    }
}
