package com.project.toosung_back.domain.auth.service.oauth;

import com.project.toosung_back.domain.auth.converter.AuthConverter;
import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.entity.SocialAuth;
import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.domain.auth.repository.SocialAuthRepository;
import com.project.toosung_back.domain.auth.service.oauth.kakao.KakaoOAuthService;
import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import com.project.toosung_back.global.security.userdetails.CustomUserDetails;
import com.project.toosung_back.global.security.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OAuthService {

    private final MemberRepository memberRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final JwtUtil jwtUtil;
    private final KakaoOAuthService kakaoOAuthService;

    // 카카오 로그인
    public OAuthResDTO.LoginResponse kakaoLogin(String code) {
        // 1. 인가코드로 토큰 발급
        OAuthResDTO.KakaoTokenResponse tokenResponse = kakaoOAuthService.getToken(code);

        // 2. 토큰으로 사용자 정보 조회
        OAuthResDTO.KakaoUserInfo kakaoUserInfo = kakaoOAuthService.getUserInfo(tokenResponse.accessToken());

        // 3. 회원 조회 or 신규 가입
        Member member = findOrCreateMember(kakaoUserInfo);

        return createLoginResponse(member);
    }

    private Member findOrCreateMember(OAuthResDTO.KakaoUserInfo userInfo) {
        String providerId = String.valueOf(userInfo.id());

        return socialAuthRepository.findMemberByProviderAndProviderId(Provider.KAKAO, providerId)
                .orElseGet(() -> createMember(userInfo));
    }

    private Member createMember(OAuthResDTO.KakaoUserInfo userInfo) {
        Member member = memberRepository.save(AuthConverter.toKakaoMember(userInfo));

        socialAuthRepository.save(AuthConverter.toSocialAuth(userInfo, Provider.KAKAO, member));

        return member;
    }

    private OAuthResDTO.LoginResponse createLoginResponse(Member member) {
        CustomUserDetails userDetails = new CustomUserDetails(member);

        String accessToken = jwtUtil.createAccessToken(userDetails);
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        return AuthConverter.toLoginResponse(member, accessToken, refreshToken);
    }
}
