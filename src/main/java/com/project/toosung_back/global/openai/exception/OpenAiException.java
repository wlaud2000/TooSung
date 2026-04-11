package com.project.toosung_back.global.openai.exception;

import com.project.toosung_back.global.apiPayload.exception.CustomException;

public class OpenAiException extends CustomException {

    public OpenAiException(OpenAiErrorCode errorCode) {
        super(errorCode);
    }
}
