package com.project.toosung_back.domain.news.controller;

import com.project.toosung_back.domain.news.controller.docs.RealEstateNewsDocs;
import com.project.toosung_back.domain.news.dto.response.RealEstateNewsResDTO;
import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.domain.news.service.query.RealEstateNewsQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/news")
public class RealEstateNewsController implements RealEstateNewsDocs {

    private final RealEstateNewsQueryService realEstateNewsQueryService;

    @Override
    @GetMapping("/realestate")
    public CustomResponse<RealEstateNewsResDTO.RealEstateNewsList> getRealEstateNews(
            @RequestParam String region,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sentiment sentimentEnum = null;
        if (sentiment != null) {
            try {
                sentimentEnum = Sentiment.valueOf(sentiment.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "올바르지 않은 sentiment 값입니다. POSITIVE / NEGATIVE / NEUTRAL 중 하나를 입력하세요.");
            }
        }

        RealEstateNewsResDTO.RealEstateNewsList result =
                realEstateNewsQueryService.getRealEstateNews(region, sentimentEnum, cursor, size);

        return CustomResponse.onSuccess("부동산 뉴스 조회 성공", result);
    }
}
