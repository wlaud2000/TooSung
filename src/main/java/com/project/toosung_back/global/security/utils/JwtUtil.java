package com.project.toosung_back.global.security.utils;

import com.project.toosung_back.global.security.dto.JwtDTO;
import com.project.toosung_back.global.security.dto.TokenInfo;
import com.project.toosung_back.global.security.userdetails.CustomUserDetails;
import com.project.toosung_back.global.utils.RedisUtil;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Long accessExpMs;
    private final Long refreshExpMs;
    private final RedisUtil redisUtil;

    public JwtUtil(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.token.access-expiration-time}") Long accessExpMs,
            @Value("${spring.jwt.token.refresh-expiration-time}") Long refreshExpMs,
            RedisUtil redisUtil
    ) {
        this.secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
        this.redisUtil = redisUtil;
    }

    /**
     * JWT 토큰 생성
     */
    public String createToken(CustomUserDetails userDetails, Instant expirationTime) {
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .and()
                .subject(userDetails.getEmail())
                .claim("userId", userDetails.getMemberId())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expirationTime))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(CustomUserDetails userDetails) {
        Instant expiration = Instant.now().plusMillis(accessExpMs);
        return createToken(userDetails, expiration);
    }

    /**
     * Refresh Token 생성 + Redis 저장
     */
    public String createRefreshToken(CustomUserDetails userDetails) {
        Instant expiration = Instant.now().plusMillis(refreshExpMs);
        String refreshToken = createToken(userDetails, expiration);

        // Redis에 저장
        redisUtil.save(
                "refresh:" + userDetails.getEmail(),
                refreshToken,
                refreshExpMs,
                TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    /**
     * 토큰 검증 (블랙리스트 체크 포함)
     */
    public void validateToken(String token) {
        // 1. 블랙리스트 체크
        if (redisUtil.hasKey("blacklist:" + token)) {
            throw new SecurityException("무효화된 토큰입니다.");
        }

        // 2. 토큰 유효성 검증
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .clockSkewSeconds(180) // 3분 오차 허용
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtException(null, null, "만료된 토큰입니다.");
        } catch (SecurityException | MalformedJwtException e) {
            throw new SecurityException("잘못된 토큰 서명입니다.");
        } catch (UnsupportedJwtException e) {
            throw new UnsupportedJwtException("지원하지 않는 토큰입니다.");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("토큰이 비어있습니다.");
        }
    }

    /**
     * 토큰 파싱 (Claims 추출)
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 정보 추출 (한 번에)
     */
    public TokenInfo extractTokenInfo(String token) {
        Claims claims = parseToken(token);
        return new TokenInfo(
                claims.getSubject(),
                claims.get("userId", Long.class)
        );
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmail(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * 토큰 남은 만료시간 (초)
     */
    public long getRemainingTime(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingMs / 1000);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }

    /**
     * 토큰 재발급 (RTR 패턴 적용)
     */
    public JwtDTO reissueToken(String refreshToken) {
        // 1. 토큰에서 이메일 추출
        String email = getEmail(refreshToken);

        // 2. Redis에 저장된 토큰과 비교 (RTR)
        String savedToken = redisUtil.get("refresh:" + email);
        if (savedToken == null || !refreshToken.equals(savedToken)) {
            // 토큰 재사용 감지 → 모든 토큰 무효화
            redisUtil.delete("refresh:" + email);
            throw new SecurityException("토큰 재사용이 감지되었습니다. 다시 로그인해주세요.");
        }

        // 3. 기존 Refresh Token 삭제
        redisUtil.delete("refresh:" + email);

        // 4. 토큰 정보로 새 토큰 발급
        TokenInfo tokenInfo = extractTokenInfo(refreshToken);
        CustomUserDetails userDetails = CustomUserDetails.fromTokenInfo(tokenInfo);

        return JwtDTO.builder()
                .accessToken(createAccessToken(userDetails))
                .refreshToken(createRefreshToken(userDetails))
                .build();
    }

    /**
     * 토큰 블랙리스트 등록
     */
    public void addToBlacklist(String token, long remainingTimeSeconds) {
        if (remainingTimeSeconds > 0) {
            redisUtil.save(
                    "blacklist:" + token,
                    "logout",
                    remainingTimeSeconds,
                    TimeUnit.SECONDS
            );
        }
    }

    public Long getAccessExpMs() {
        return accessExpMs;
    }

    public Long getRefreshExpMs() {
        return refreshExpMs;
    }
}
