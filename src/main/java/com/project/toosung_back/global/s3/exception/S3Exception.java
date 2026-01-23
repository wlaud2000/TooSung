package com.project.toosung_back.global.s3.exception;

import com.project.toosung_back.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class S3Exception extends CustomException {

    public S3Exception(S3ErrorCode errorCode) {
        super(errorCode);
    }
}
