package com.project.toosung_back.domain.feed.controller;

import com.project.toosung_back.domain.feed.controller.docs.FeedDocs;
import com.project.toosung_back.domain.feed.dto.response.FeedResDTO;
import com.project.toosung_back.domain.feed.service.FeedQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.annotation.CurrentUser;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feed")
public class FeedController implements FeedDocs {

    private final FeedQueryService feedQueryService;

    @Override
    @GetMapping
    public CustomResponse<FeedResDTO.FeedList> getFeed(
            @CurrentUser AuthUser authUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        FeedResDTO.FeedList dto = feedQueryService.getFeed(authUser.getMemberId(), cursor, size);
        return CustomResponse.onSuccess("통합 피드 조회 성공", dto);
    }
}
