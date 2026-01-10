package com.project.toosung_back.global.security.exception;

import com.project.toosung_back.global.utils.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("인증 실패: {}", authException.getMessage());

        SecurityErrorCode errorCode = SecurityErrorCode.UNAUTHORIZED;
        HttpResponseUtil.setErrorResponse(
                response,
                errorCode.getHttpStatus(),
                errorCode.getErrorResponse()
        );
    }
}

