package com.project.toosung_back.domain.stock.service.query;

import com.project.toosung_back.domain.stock.converter.StockConverter;
import com.project.toosung_back.domain.stock.dto.response.StockResDTO;
import com.project.toosung_back.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockQueryService {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public StockResDTO.ResStockSearchList searchStocks(String keyword) {
        List<StockResDTO.StockItem> items = stockRepository.searchByKeyword(keyword)
                .stream()
                .map(StockConverter::toStockItem)
                .toList();

        return StockResDTO.ResStockSearchList.builder()
                .stocks(items)
                .build();
    }
}
