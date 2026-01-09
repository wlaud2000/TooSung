package com.project.toosung_back.domain.auth.converter;

import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.entity.LocalAuth;
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

    public static LocalAuth toLocalAuth(Member member, String rawPassword, PasswordEncoder passwordEncoder) {
        return LocalAuth.builder()
                .member(member)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
    }

    public static AuthResDTO.ResSignUp toResSignUp(Member member) {
        return AuthResDTO.ResSignUp.builder()
                .memberId(member.getId())
                .build();
    }
}
