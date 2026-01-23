package com.project.toosung_back.domain.member.dto.response;

import lombok.Builder;

public class MemberResDTO {

    @Builder
    public record ResMemberInfo(
            String email,
            String nickname,
            String profileImageUrl,
            boolean isSocialLogin
    ) {}
}
