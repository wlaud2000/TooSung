package com.project.toosung_back.global.security.dto;

public record TokenInfo(
        String email,
        Long memberId
) {}
