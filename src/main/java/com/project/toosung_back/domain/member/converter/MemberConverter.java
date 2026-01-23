package com.project.toosung_back.domain.member.converter;

import com.project.toosung_back.domain.member.dto.response.MemberResDTO;
import com.project.toosung_back.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberConverter {

    public static MemberResDTO.ResMemberInfo toResMemberInfo(Member member) {
        return MemberResDTO.ResMemberInfo.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .isSocialLogin(member.isSocialLogin())
                .build();
    }
}
