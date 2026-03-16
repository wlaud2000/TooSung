package com.project.toosung_back.domain.stock.dto.response;

import lombok.Builder;

import java.util.List;

public class StockResDTO {

    @Builder
    public record StockItem(
            Long stockId,
            String name,
            String code,
            String market
    ) {}

    @Builder
    public record ResStockSearchList(
            List<StockItem> stocks
    ) {}
}
