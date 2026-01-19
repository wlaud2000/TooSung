package com.project.toosung_back.domain.auth.dto.response;

import com.project.toosung_back.domain.auth.enums.Provider;
import lombok.Builder;

/**
 * 모든 OAuth Provider의 사용자 정보를 통합하는 DTO
 */
@Builder
public record OAuthUserInfo(
        Provider provider,
        String providerId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
