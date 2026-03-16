package com.project.toosung_back.domain.watchlist.exception;

import com.project.toosung_back.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class WatchlistException extends CustomException {

    public WatchlistException(WatchlistErrorCode errorCode) {
        super(errorCode);
    }
}
