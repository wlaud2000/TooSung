package com.project.toosung_back.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.dto.JwtDTO;
import com.project.toosung_back.global.security.exception.SecurityErrorCode;
import com.project.toosung_back.global.security.userdetails.CustomUserDetails;
import com.project.toosung_back.global.security.utils.CookieUtil;
import com.project.toosung_back.global.security.utils.JwtUtil;
import com.project.toosung_back.global.utils.HttpResponseUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws AuthenticationException {
        log.debug("로그인 시도");

        try {
            AuthReqDTO.Login loginRequest = new ObjectMapper()
                    .readValue(request.getInputStream(), AuthReqDTO.Login.class);

            String email = loginRequest.email();
            String password = loginRequest.password();

            log.debug("로그인 이메일: {}", email);
            // 비밀번호는 절대 로깅하지 않음!

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(email, password, null);

            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            log.error("로그인 요청 파싱 실패: {}", e.getMessage());
            throw new AuthenticationServiceException("로그인 요청을 처리할 수 없습니다.");
        }
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authentication
    ) throws IOException {
        log.info("로그인 성공");

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(userDetails);
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        // 쿠키에 저장
        cookieUtil.addCookie(response, "access_token", accessToken, jwtUtil.getAccessExpMs());
        cookieUtil.addCookie(response, "refresh_token", refreshToken, jwtUtil.getRefreshExpMs());

        // 응답 바디에도 포함 (선택사항)
        JwtDTO jwtDTO = JwtDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        HttpResponseUtil.setSuccessResponse(response, HttpStatus.OK, jwtDTO);
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed
    ) throws IOException {
        log.warn("로그인 실패: {}", failed.getMessage());

        SecurityErrorCode errorCode = mapToErrorCode(failed);

        HttpResponseUtil.setErrorResponse(
                response,
                errorCode.getHttpStatus(),
                CustomResponse.onFailure(errorCode.getCode(), errorCode.getMessage())
        );
    }

    private SecurityErrorCode mapToErrorCode(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return SecurityErrorCode.BAD_CREDENTIALS;
        }
        if (exception instanceof UsernameNotFoundException) {
            return SecurityErrorCode.USER_NOT_FOUND;
        }
        if (exception instanceof LockedException || exception instanceof DisabledException) {
            return SecurityErrorCode.FORBIDDEN;
        }
        if (exception instanceof AuthenticationServiceException) {
            return SecurityErrorCode.INTERNAL_SECURITY_ERROR;
        }
        return SecurityErrorCode.UNAUTHORIZED;
    }
}
