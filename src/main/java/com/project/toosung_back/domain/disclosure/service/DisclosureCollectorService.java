package com.project.toosung_back.domain.disclosure.service;

import com.project.toosung_back.domain.disclosure.client.DartApiClient;
import com.project.toosung_back.domain.disclosure.client.dto.DartDisclosureListResponse;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.entity.DisclosureSource;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
import com.project.toosung_back.global.cache.CacheEvictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureCollectorService {

    private final WatchlistRepository watchlistRepository;
    private final DisclosureRepository disclosureRepository;
    private final DartApiClient dartApiClient;
    private final CacheEvictService cacheEvictService;

    private static final List<String> PBLNTF_TY_LIST = List.of("B", "A");

    private static final Map<String, String> KEYWORD_TO_DS005_ENDPOINT = Map.of(
            "유상증자", "/api/piicDecsn.json",
            "합병", "/api/mrgrDecsn.json",
            "자기주식", "/api/tcbDecsn.json",
            "전환사채", "/api/cvbdIsDecsn.json",
            "분할", "/api/dssDecsn.json"
    );

    private static final List<String> REPORT_KEYWORDS = List.copyOf(KEYWORD_TO_DS005_ENDPOINT.keySet());

    public void collectAll() {
        collectForDate(LocalDate.now());
    }

    public void collectForDate(LocalDate date) {
        List<Stock> stocks = watchlistRepository.findAllDistinctStocks();
        if (stocks.isEmpty()) {
            log.info("[DisclosureCollectorService] 관심 종목 없음. 수집 스킵");
            return;
        }

        Map<String, Stock> symbolToStock = stocks.stream()
                .collect(Collectors.toMap(Stock::getSymbol, s -> s));

        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        log.info("[DisclosureCollectorService] 수집 시작: date={}, 관심종목={}개", dateStr, symbolToStock.size());

        int savedCount = 0;
        for (String pblntfTy : PBLNTF_TY_LIST) {
            List<DartDisclosureListResponse.DartItem> items = dartApiClient.fetchDisclosureList(pblntfTy, dateStr);
            for (DartDisclosureListResponse.DartItem item : items) {
                if (processItem(item, symbolToStock, dateStr)) {
                    savedCount++;
                }
            }
        }

        log.info("[DisclosureCollectorService] 수집 완료: date={}, {}건 저장", dateStr, savedCount);

        stocks.forEach(stock -> cacheEvictService.evictDisclosureCache(stock.getId()));
    }

    private boolean processItem(DartDisclosureListResponse.DartItem item,
                                Map<String, Stock> symbolToStock,
                                String today) {
        if (item.stockCode() == null || item.stockCode().isBlank()) return false;

        Stock stock = symbolToStock.get(item.stockCode());
        if (stock == null) return false;

        if (disclosureRepository.existsByDartId(item.rceptNo())) return false;

        LocalDateTime publishedAt = LocalDate
                .parse(item.rceptDt(), DateTimeFormatter.BASIC_ISO_DATE)
                .atStartOfDay();

        Disclosure disclosure = disclosureRepository.save(
                Disclosure.builder()
                        .dartId(item.rceptNo())
                        .disclosureType(item.reportNm())
                        .url("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + item.rceptNo())
                        .publishedAt(publishedAt)
                        .source(DisclosureSource.DART)
                        .stock(stock)
                        .build()
        );

        String matchedKeyword = matchKeyword(item.reportNm());
        String ds005Endpoint = matchedKeyword != null ? KEYWORD_TO_DS005_ENDPOINT.get(matchedKeyword) : null;
        if (ds005Endpoint != null) {
            if (stock.getDartCorpCode() == null) {
                log.warn("[DisclosureCollectorService] dart_corp_code 없음 - DS005 스킵: symbol={}", stock.getSymbol());
            } else {
                String rawData = dartApiClient.fetchDisclosureDetailRaw(ds005Endpoint, stock.getDartCorpCode(), today);
                if (rawData != null) {
                    disclosure.updateRawData(rawData);
                    disclosureRepository.save(disclosure);
                }
            }
        }

        log.info("[DisclosureCollectorService] 저장 완료: dartId={}, type={}, stock={}",
                item.rceptNo(), item.reportNm(), stock.getSymbol());
        return true;
    }

    private String matchKeyword(String reportNm) {
        if (reportNm == null) return null;
        for (String keyword : REPORT_KEYWORDS) {
            if (reportNm.contains(keyword)) return keyword;
        }
        return null;
    }
}
