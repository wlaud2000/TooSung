package com.project.toosung_back.global.openai.exception;

import com.project.toosung_back.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum OpenAiErrorCode implements BaseErrorCode {

    API_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI500_1", "OpenAI API 호출에 실패했습니다"),
    RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI500_2", "OpenAI 응답 파싱에 실패했습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "OPENAI401_1", "OpenAI API 인증에 실패했습니다"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "OPENAI429_1", "OpenAI API 호출 한도를 초과했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
