package com.project.toosung_back.domain.auth.converter;

import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.entity.LocalAuth;
import com.project.toosung_back.domain.auth.entity.SocialAuth;
import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthConverter {

    public static Member toMember(AuthReqDTO.ReqSignUp reqDTO) {
        return Member.builder()
                .email(reqDTO.email())
                .nickname(reqDTO.nickname())
                .build();
    }

    public static Member toKakaoMember(OAuthResDTO.KakaoUserInfo userInfo) {
        return Member.builder()
                .email(userInfo.getEmail())
                .nickname(userInfo.getNickname())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .build();
    }

    public static LocalAuth toLocalAuth(Member member, String rawPassword, PasswordEncoder passwordEncoder) {
        return LocalAuth.builder()
                .member(member)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
    }

    public static SocialAuth toSocialAuth(OAuthResDTO.KakaoUserInfo userInfo, Provider provider, Member member) {
        return SocialAuth.builder()
                .provider(provider)
                .providerId(String.valueOf(userInfo.id()))
                .member(member)
                .build();
    }

    public static AuthResDTO.ResSignUp toResSignUp(Member member) {
        return AuthResDTO.ResSignUp.builder()
                .memberId(member.getId())
                .build();
    }

    public static OAuthResDTO.LoginResponse toLoginResponse(Member member, String accessToken, String refreshToken) {
        return OAuthResDTO.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .memberInfo(toMemberInfo(member))
                .build();
    }

    public static OAuthResDTO.LoginResponse.MemberInfo toMemberInfo(Member member) {
        return OAuthResDTO.LoginResponse.MemberInfo.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .build();
    }

}
