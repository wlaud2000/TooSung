package com.project.toosung_back.domain.news.exception;

import com.project.toosung_back.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum NewsErrorCode implements BaseErrorCode {

    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "NEWS404_1", "존재하지 않는 뉴스입니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
