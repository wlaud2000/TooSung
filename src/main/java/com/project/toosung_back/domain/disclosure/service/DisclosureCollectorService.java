package com.project.toosung_back.domain.disclosure.service;

import com.project.toosung_back.domain.disclosure.client.DartApiClient;
import com.project.toosung_back.domain.disclosure.client.dto.DartDisclosureListResponse;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.repository.WatchlistRepository;
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

    private static final List<String> PBLNTF_TY_LIST = List.of("B", "A");

    // report_nm 포함 키워드 (순서 중요: 더 구체적인 키워드를 앞에)
    private static final List<String> REPORT_KEYWORDS = List.of(
            "유상증자", "합병", "자기주식", "전환사채", "분할",
            "사업보고서", "분기보고서", "반기보고서"
    );

    // 키워드 → DS005 엔드포인트 (없으면 DS001 데이터만 저장)
    private static final Map<String, String> KEYWORD_TO_DS005_ENDPOINT = Map.of(
            "유상증자", "/api/piicDecsn.json",
            "합병", "/api/mrgrDecsn.json",
            "자기주식", "/api/tcbDecsn.json",
            "전환사채", "/api/cvbdIsDecsn.json",
            "분할", "/api/dssDecsn.json"
    );

    public void collectAll() {
        List<Stock> stocks = watchlistRepository.findAllDistinctStocks();
        if (stocks.isEmpty()) {
            log.info("[DisclosureCollectorService] 관심 종목 없음. 수집 스킵");
            return;
        }

        Map<String, Stock> symbolToStock = stocks.stream()
                .collect(Collectors.toMap(Stock::getSymbol, s -> s));

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        log.info("[DisclosureCollectorService] 수집 시작: date={}, 관심종목={}개", today, symbolToStock.size());

        int savedCount = 0;
        for (String pblntfTy : PBLNTF_TY_LIST) {
            List<DartDisclosureListResponse.DartItem> items = dartApiClient.fetchDisclosureList(pblntfTy, today);
            for (DartDisclosureListResponse.DartItem item : items) {
                if (processItem(item, symbolToStock, today)) {
                    savedCount++;
                }
            }
        }

        log.info("[DisclosureCollectorService] 수집 완료: {}건 저장", savedCount);
    }

    private boolean processItem(DartDisclosureListResponse.DartItem item,
                                Map<String, Stock> symbolToStock,
                                String today) {
        // 1. 비상장 법인 필터 (stock_code 없음)
        if (item.stockCode() == null || item.stockCode().isBlank()) return false;

        // 2. 관심종목 필터
        Stock stock = symbolToStock.get(item.stockCode());
        if (stock == null) return false;

        // 3. 공시 유형 키워드 필터
        String matchedKeyword = matchKeyword(item.reportNm());
        if (matchedKeyword == null) return false;

        // 4. 중복 체크
        if (disclosureRepository.existsByDartId(item.rceptNo())) return false;

        // 5. Disclosure 기본 정보 저장
        LocalDateTime publishedAt = LocalDate
                .parse(item.rceptDt(), DateTimeFormatter.BASIC_ISO_DATE)
                .atStartOfDay();

        Disclosure disclosure = disclosureRepository.save(
                Disclosure.builder()
                        .dartId(item.rceptNo())
                        .disclosureType(item.reportNm())
                        .url("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + item.rceptNo())
                        .publishedAt(publishedAt)
                        .stock(stock)
                        .build()
        );

        // 6. DS005 세부 데이터 수집 (해당하는 공시 유형만)
        String ds005Endpoint = KEYWORD_TO_DS005_ENDPOINT.get(matchedKeyword);
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
        for (String keyword : REPORT_KEYWORDS) {
            if (reportNm.contains(keyword)) return keyword;
        }
        return null;
    }
}
