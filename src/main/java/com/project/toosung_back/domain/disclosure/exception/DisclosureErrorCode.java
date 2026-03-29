package com.project.toosung_back.domain.disclosure.exception;

import com.project.toosung_back.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum DisclosureErrorCode implements BaseErrorCode {

    DISCLOSURE_NOT_FOUND(HttpStatus.NOT_FOUND, "DISCLOSURE404_1", "존재하지 않는 공시입니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
