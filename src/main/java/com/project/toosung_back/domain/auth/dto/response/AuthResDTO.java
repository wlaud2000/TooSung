package com.project.toosung_back.domain.auth.dto.response;

import lombok.Builder;

public class AuthResDTO {

    @Builder
    public record ResSignUp(
            Long memberId
    ) {}
}
