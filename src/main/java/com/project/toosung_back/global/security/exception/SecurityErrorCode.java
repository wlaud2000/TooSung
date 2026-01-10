package com.project.toosung_back.global.security.exception;

import com.project.toosung_back.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SecurityErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "SEC-001", "잘못된 토큰입니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "SEC-002", "인증되지 않은 사용자입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "SEC-003", "토큰이 만료되었습니다."),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "SEC-004", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "SEC-005", "접근 권한이 없습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "SEC-006", "존재하지 않는 사용자입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "SEC-007", "리프레시 토큰이 존재하지 않습니다."),

    // 500 Internal Server Error
    INTERNAL_SECURITY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SEC-999", "인증 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
