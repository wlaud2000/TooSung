package com.project.toosung_back.domain.auth.service;

import com.project.toosung_back.domain.auth.converter.AuthConverter;
import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.exception.AuthErrorCode;
import com.project.toosung_back.domain.auth.repository.LocalAuthRepository;
import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import com.project.toosung_back.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final LocalAuthRepository localAuthRepository;
    private final PasswordEncoder passwordEncoder;

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
}
