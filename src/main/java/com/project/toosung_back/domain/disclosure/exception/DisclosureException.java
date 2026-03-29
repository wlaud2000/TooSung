package com.project.toosung_back.domain.disclosure.exception;

import com.project.toosung_back.global.apiPayload.exception.CustomException;

public class DisclosureException extends CustomException {
    public DisclosureException(DisclosureErrorCode errorCode) {
        super(errorCode);
    }
}
