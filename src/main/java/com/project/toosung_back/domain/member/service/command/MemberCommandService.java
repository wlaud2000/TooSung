package com.project.toosung_back.domain.member.service.command;

import com.project.toosung_back.domain.member.converter.MemberConverter;
import com.project.toosung_back.domain.member.dto.request.MemberReqDTO;
import com.project.toosung_back.domain.member.dto.response.MemberResDTO;
import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.member.exception.MemberErrorCode;
import com.project.toosung_back.domain.member.exception.MemberException;
import com.project.toosung_back.domain.member.repository.MemberRepository;
import com.project.toosung_back.global.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final S3UploadService s3UploadService;

    public MemberResDTO.ResMemberInfo updateProfile(Long memberId, MemberReqDTO.ReqUpdateProfile reqDTO) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.isDeleted()) {
            throw new MemberException(MemberErrorCode.MEMBER_ALREADY_DELETED);
        }

        String newProfileImageUrl = null;

        if (reqDTO.profileImageUrl() != null && !reqDTO.profileImageUrl().isBlank()) {
            // 기존 프로필 이미지 삭제
            s3UploadService.deleteFile(member.getProfileImageUrl());
            newProfileImageUrl = reqDTO.profileImageUrl();
        }

        member.updateProfile(reqDTO.nickname(), newProfileImageUrl);

        return MemberConverter.toResMemberInfo(member);
    }
}
