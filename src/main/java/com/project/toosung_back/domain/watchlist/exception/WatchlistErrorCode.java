package com.project.toosung_back.domain.watchlist.exception;

import com.project.toosung_back.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatchlistErrorCode implements BaseErrorCode {

    WATCHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "WATCHLIST-001", "존재하지 않는 관심 종목입니다."),
    WATCHLIST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "WATCHLIST-002", "해당 관심 종목에 접근할 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
