package com.project.toosung_back.domain.stock.controller;

import com.project.toosung_back.domain.stock.controller.docs.StockDocs;
import com.project.toosung_back.domain.stock.dto.response.StockResDTO;
import com.project.toosung_back.domain.stock.service.query.StockQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks")
public class StockController implements StockDocs {

    private final StockQueryService stockQueryService;

    @Override
    @GetMapping("/search")
    public CustomResponse<StockResDTO.ResStockSearchList> searchStocks(
            @RequestParam String keyword
    ) {
        StockResDTO.ResStockSearchList resDTO = stockQueryService.searchStocks(keyword);
        return CustomResponse.onSuccess("종목 검색 성공", resDTO);
    }
}
