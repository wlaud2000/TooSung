package com.project.toosung_back.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

public class OAuthResDTO {

    public record KakaoTokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("expires_in")
            Integer expiresIn
    ) {}

    public record KakaoUserInfo(
            Long id,

            @JsonProperty("kakao_account")
            KakaoAccount kakaoAccount
    ) {
        public record KakaoAccount(
                String email,
                Profile profile
        ) {
            public record Profile(
                    String nickname,

                    @JsonProperty("profile_image_url")
                    String profileImageUrl
            ) {}
        }

        public String getEmail() {
            return kakaoAccount != null ? kakaoAccount.email() : null;
        }

        public String getNickname() {
            if (kakaoAccount != null && kakaoAccount.profile() != null) {
                return kakaoAccount.profile().nickname();
            }
            return null;
        }

        public String getProfileImageUrl() {
            if (kakaoAccount != null && kakaoAccount.profile() != null) {
                return kakaoAccount.profile().profileImageUrl();
            }
            return null;
        }
    }

    @Builder
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            MemberInfo memberInfo
    ) {
        @Builder
        public record MemberInfo(
                Long id,
                String email,
                String nickname,
                String profileImageUrl
        ) {}
    }
}
