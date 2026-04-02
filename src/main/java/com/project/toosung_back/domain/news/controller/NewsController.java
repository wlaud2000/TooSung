package com.project.toosung_back.domain.news.controller;

import com.project.toosung_back.domain.news.controller.docs.NewsDocs;
import com.project.toosung_back.domain.news.dto.response.NewsResDTO;
import com.project.toosung_back.domain.news.service.query.NewsQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news")
public class NewsController implements NewsDocs {

    private final NewsQueryService newsQueryService;

    @Override
    @GetMapping
    public CustomResponse<NewsResDTO.NewsList> getNews(
            @RequestParam Long stockId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        NewsResDTO.NewsList dto = newsQueryService.getNews(stockId, cursor, size);
        return CustomResponse.onSuccess("뉴스 목록 조회 성공", dto);
    }
}