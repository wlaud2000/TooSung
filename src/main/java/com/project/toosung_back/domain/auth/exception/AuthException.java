package com.project.toosung_back.domain.auth.exception;

import com.project.toosung_back.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class AuthException extends CustomException {

    public AuthException(AuthErrorCode errorCode){
        super(errorCode);
    }
}
