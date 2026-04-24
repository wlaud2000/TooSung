package com.project.toosung_back.domain.briefing.controller;

import com.project.toosung_back.domain.briefing.controller.docs.BriefingDocs;
import com.project.toosung_back.domain.briefing.dto.response.BriefingResDTO;
import com.project.toosung_back.domain.briefing.service.query.BriefingQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.annotation.CurrentUser;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/briefing")
public class BriefingController implements BriefingDocs {

    private final BriefingQueryService briefingQueryService;

    @Override
    @GetMapping("/today")
    public CustomResponse<BriefingResDTO.BriefingDetail> getTodayBriefing(@CurrentUser AuthUser authUser) {
        BriefingResDTO.BriefingDetail dto = briefingQueryService.getTodayBriefing(authUser.getMemberId());
        return CustomResponse.onSuccess("오늘의 브리핑 조회 성공", dto);
    }
}