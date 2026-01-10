package com.project.toosung_back.global.security.filter;

import com.project.toosung_back.global.security.dto.TokenInfo;
import com.project.toosung_back.global.security.exception.SecurityErrorCode;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import com.project.toosung_back.global.security.userdetails.CustomUserDetails;
import com.project.toosung_back.global.security.utils.CookieUtil;
import com.project.toosung_back.global.security.utils.JwtUtil;
import com.project.toosung_back.global.utils.HttpResponseUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.debug("JWT 인가 필터 - URI: {}", requestURI);

        // 1. 쿠키에서 Access Token 추출
        String accessToken = cookieUtil.extractFromCookie(request, "access_token");

        if (accessToken == null) {
            log.debug("Access Token 없음 - 필터 통과");
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 토큰 검증 및 인증
        try {
            jwtUtil.validateToken(accessToken);
            setAuthentication(accessToken);
            log.debug("JWT 인증 성공");

        } catch (ExpiredJwtException e) {
            log.warn("Access Token 만료");
            handleException(response, SecurityErrorCode.TOKEN_EXPIRED);
            return;

        } catch (SecurityException e) {
            log.warn("무효화된 토큰: {}", e.getMessage());
            handleException(response, SecurityErrorCode.INVALID_TOKEN);
            return;

        } catch (Exception e) {
            log.warn("JWT 인증 실패: {}", e.getMessage());
            handleException(response, SecurityErrorCode.INVALID_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * SecurityContext에 인증 정보 저장 (DB 조회 없이)
     */
    private void setAuthentication(String accessToken) {
        TokenInfo tokenInfo = jwtUtil.extractTokenInfo(accessToken);

        AuthUser authUser = new AuthUser(
                tokenInfo.memberId(),
                tokenInfo.email(),
                null
        );

        CustomUserDetails userDetails = new CustomUserDetails(authUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void handleException(HttpServletResponse response, SecurityErrorCode errorCode) throws IOException {
        HttpResponseUtil.setErrorResponse(
                response,
                errorCode.getHttpStatus(),
                errorCode.getErrorResponse()
        );
    }
}
