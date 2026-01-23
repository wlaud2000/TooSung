package com.project.toosung_back.domain.member.exception;

import com.project.toosung_back.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-001", "존재하지 않는 회원입니다."),
    MEMBER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "MEMBER-002", "이미 탈퇴한 회원입니다."),
    MEMBER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEMBER-003", "해당 회원 정보에 접근할 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
