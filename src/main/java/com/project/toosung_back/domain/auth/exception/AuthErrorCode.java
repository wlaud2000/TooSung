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

    INVALID_AUTHORIZATION_CODE(HttpStatus.BAD_REQUEST, "OAUTH_001", "유효하지 않은 인가 코드입니다."),
    TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "OAUTH_002", "소셜 로그인 토큰 발급에 실패했습니다."),
    USER_INFO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "OAUTH_003", "소셜 로그인 사용자 정보 조회에 실패했습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH_004", "지원하지 않는 소셜 로그인입니다."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "OAUTH_005", "유효하지 않은 state 값입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
