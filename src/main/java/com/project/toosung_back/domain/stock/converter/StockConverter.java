package com.project.toosung_back.domain.stock.converter;

import com.project.toosung_back.domain.stock.dto.response.StockResDTO;
import com.project.toosung_back.domain.stock.entity.Stock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StockConverter {

    public static StockResDTO.StockItem toStockItem(Stock stock) {
        return StockResDTO.StockItem.builder()
                .stockId(stock.getId())
                .name(stock.getName())
                .code(stock.getSymbol())
                .market(stock.getMarket())
                .build();
    }
}
