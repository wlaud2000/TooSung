package com.project.toosung_back.domain.news.exception;

import com.project.toosung_back.global.apiPayload.exception.CustomException;

public class NewsException extends CustomException {
    public NewsException(NewsErrorCode errorCode) {
        super(errorCode);
    }
}
