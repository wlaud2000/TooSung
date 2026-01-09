package com.project.toosung_back.domain.auth.exception;

import com.project.toosung_back.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH-001", "이미 사용 중인 이메일입니다."),
    ALREADY_SOCIAL_MEMBER(HttpStatus.CONFLICT, "AUTH-002", "소셜 로그인으로 가입된 이메일입니다. 해당 소셜 계정으로 로그인해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
