package com.project.toosung_back.domain.auth.dto.request;

import com.project.toosung_back.domain.auth.enums.Provider;

public class OAuthReqDTO {

    public record OAuthLoginRequest(
            Provider provider,
            String code
    ) {}
}
