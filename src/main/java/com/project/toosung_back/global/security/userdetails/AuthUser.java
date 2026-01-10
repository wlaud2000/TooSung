package com.project.toosung_back.global.security.userdetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthUser {

    private final Long memberId;
    private final String email;

    @JsonIgnore
    private final String password;
}
