package com.project.toosung_back.domain.auth.service;

import com.project.toosung_back.domain.auth.converter.AuthConverter;
import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.exception.AuthErrorCode;
import com.project.toosung_back.domain.auth.repository.LocalAuthRepository;
import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import com.project.toosung_back.global.apiPayload.exception.CustomException;
import com.project.toosung_back.global.security.dto.JwtDTO;
import com.project.toosung_back.global.security.utils.CookieUtil;
import com.project.toosung_back.global.security.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final LocalAuthRepository localAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    @Transactional
    public AuthResDTO.ResSignUp signUp(AuthReqDTO.ReqSignUp reqDTO) {
        // 이메일 중복 검증
        memberRepository.findByEmail(reqDTO.email())
                .ifPresent(member -> {
                    if (localAuthRepository.existsByMember(member)) {
                        throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
                    }
                    throw new CustomException(AuthErrorCode.ALREADY_SOCIAL_MEMBER);
                });

        // Member 저장
        Member savedMember = memberRepository.save(AuthConverter.toMember(reqDTO));

        // LocalAuth 저장
        localAuthRepository.save(
                AuthConverter.toLocalAuth(savedMember, reqDTO.password(), passwordEncoder)
        );

        return AuthConverter.toResSignUp(savedMember);
    }

    /**
     * 토큰 재발급 (RTR 패턴 적용)
     *
     * 1. 쿠키에서 Refresh Token 추출
     * 2. Refresh Token 유효성 검증
     * 3. RTR 패턴을 적용하여 새로운 Access/Refresh Token 발급
     * 4. 새로운 토큰들을 쿠키에 저장
     * 
     */
    public JwtDTO reissueToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = cookieUtil.extractFromCookie(request, "refresh_token");
        if (refreshToken == null) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 2. Refresh Token 유효성 검증
        try {
            jwtUtil.validateToken(refreshToken);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 Refresh Token으로 재발급 시도");
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        } catch (Exception e) {
            log.warn("유효하지 않은 Refresh Token: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. RTR 패턴 적용하여 새 토큰 발급
        JwtDTO newTokens;
        try {
            newTokens = jwtUtil.reissueToken(refreshToken);
        } catch (SecurityException e) {
            log.warn("토큰 재사용 감지: {}", e.getMessage());
            // 쿠키 삭제
            cookieUtil.deleteCookie(response, "access_token");
            cookieUtil.deleteCookie(response, "refresh_token");
            throw new CustomException(AuthErrorCode.TOKEN_REUSE_DETECTED);
        }

        // 4. 새 토큰을 쿠키에 저장
        cookieUtil.addCookie(response, "access_token", newTokens.accessToken(), jwtUtil.getAccessExpMs());
        cookieUtil.addCookie(response, "refresh_token", newTokens.refreshToken(), jwtUtil.getRefreshExpMs());

        log.info("토큰 재발급 완료");
        return newTokens;
    }
}
