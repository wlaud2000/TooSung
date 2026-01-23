package com.project.toosung_back.domain.member.service.query;

import com.project.toosung_back.domain.member.converter.MemberConverter;
import com.project.toosung_back.domain.member.dto.response.MemberResDTO;
import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.member.exception.MemberErrorCode;
import com.project.toosung_back.domain.member.exception.MemberException;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MemberResDTO.ResMemberInfo getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.isDeleted()) {
            throw new MemberException(MemberErrorCode.MEMBER_ALREADY_DELETED);
        }

        return MemberConverter.toResMemberInfo(member);
    }
}
