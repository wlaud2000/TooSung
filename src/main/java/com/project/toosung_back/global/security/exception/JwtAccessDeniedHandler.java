package com.project.toosung_back.global.security.exception;

import com.project.toosung_back.global.utils.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("접근 거부: {}", accessDeniedException.getMessage());

        SecurityErrorCode errorCode = SecurityErrorCode.FORBIDDEN;
        HttpResponseUtil.setErrorResponse(
                response,
                errorCode.getHttpStatus(),
                errorCode.getErrorResponse()
        );
    }
}
