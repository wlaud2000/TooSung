package com.project.toosung_back.domain.member.exception;

import com.project.toosung_back.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class MemberException extends CustomException {

    public MemberException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
