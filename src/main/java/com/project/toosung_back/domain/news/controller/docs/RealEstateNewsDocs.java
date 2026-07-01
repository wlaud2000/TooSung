package com.project.toosung_back.domain.news.controller.docs;

import com.project.toosung_back.domain.news.dto.response.RealEstateNewsResDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Tag(name = "RealEstateNews", description = "지역별 부동산 뉴스 호재/악재 분석 API")
public interface RealEstateNewsDocs {

    @Operation(
            summary = "지역별 부동산 뉴스 조회",
            description = "지역명과 감성 필터로 부동산 뉴스를 커서 기반 페이지네이션으로 조회합니다."
    )
    CustomResponse<RealEstateNewsResDTO.RealEstateNewsList> getRealEstateNews(
            @Parameter(description = "지역명 (예: 강남구, 분당신도시)", required = true) String region,
            @Parameter(description = "감성 필터 (POSITIVE/NEGATIVE/NEUTRAL), 미입력 시 전체 조회") String sentiment,
            @Parameter(description = "커서 (이전 응답의 nextCursor, 첫 페이지는 미입력, 형식: yyyy-MM-ddTHH:mm:ss)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @Parameter(description = "페이지 크기 (기본값: 10)") int size
    );
}
