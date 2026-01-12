package com.project.toosung_back.domain.auth.service.oauth.kakao;

import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.exception.AuthErrorCode;
import com.project.toosung_back.domain.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthService {

    @Value("${spring.oauth.kakao.client-id}")
    private String clientId;

    @Value("${spring.oauth.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.oauth.kakao.redirect-uri}")
    private String redirectUri;

    private final WebClient oauthWebClient;

    // 인가 코드로 Access Token 요청
    public OAuthResDTO.KakaoTokenResponse getToken(String code) {
        // 요청 파라미터
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            return oauthWebClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(params)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("카카오 토큰 요청 실패 : {}", body);
                                        return Mono.error(new AuthException(AuthErrorCode.TOKEN_REQUEST_FAILED));
                                    })
                    )
                    .bodyToMono(OAuthResDTO.KakaoTokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("카카오 토큰 요청 중 오류", e);
            throw new AuthException(AuthErrorCode.TOKEN_REQUEST_FAILED);
        }
    }

    // 액세스 토큰으로 사용자 정보 조회
    public OAuthResDTO.KakaoUserInfo getUserInfo(String accessToken) {
        try {
            return oauthWebClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
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
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 중 오류", e);
            throw new AuthException(AuthErrorCode.USER_INFO_REQUEST_FAILED);
        }
    }
}
