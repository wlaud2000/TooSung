package com.project.toosung_back.domain.member.controller;

import com.project.toosung_back.domain.member.controller.docs.MemberDocs;
import com.project.toosung_back.domain.member.dto.request.MemberReqDTO;
import com.project.toosung_back.domain.member.dto.response.MemberResDTO;
import com.project.toosung_back.domain.member.service.command.MemberCommandService;
import com.project.toosung_back.domain.member.service.query.MemberQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.annotation.CurrentUser;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController implements MemberDocs {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    @Override
    @GetMapping("/me")
    public CustomResponse<MemberResDTO.ResMemberInfo> getMyInfo(@CurrentUser AuthUser authUser) {
        MemberResDTO.ResMemberInfo resDTO = memberQueryService.getMyInfo(authUser.getMemberId());
        return CustomResponse.onSuccess("회원 정보 조회 성공", resDTO);
    }

    @Override
    @PatchMapping("/me")
    public CustomResponse<MemberResDTO.ResMemberInfo> updateProfile(
            @CurrentUser AuthUser authUser,
            @RequestBody @Valid MemberReqDTO.ReqUpdateProfile reqDTO
    ) {
        MemberResDTO.ResMemberInfo resDTO = memberCommandService.updateProfile(
                authUser.getMemberId(),
                reqDTO
        );
        return CustomResponse.onSuccess("회원 정보 수정 성공", resDTO);
    }
}
