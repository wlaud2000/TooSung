package com.project.toosung_back.domain.stock.service.command;

import com.project.toosung_back.domain.disclosure.client.DartApiClient;
import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class StockCorpCodeInitializer implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final DartApiClient dartApiClient;

    @Override
    public void run(ApplicationArguments args) {
        if (stockRepository.countByDartCorpCodeIsNull() == 0) {
            log.info("[StockCorpCodeInitializer] dart_corp_code 이미 초기화됨. 스킵");
            return;
        }

        log.info("[StockCorpCodeInitializer] dart_corp_code 초기화 시작");

        byte[] zipBytes = dartApiClient.fetchCorpCodeZip();
        if (zipBytes == null) {
            log.error("[StockCorpCodeInitializer] corpCode.xml 다운로드 실패 - dart_corp_code 초기화 중단");
            return;
        }

        Map<String, String> stockCodeToCorpCode = parseCorpCodeZip(zipBytes);
        log.info("[StockCorpCodeInitializer] DART 기업코드 파싱 완료: {}개 종목 코드 매핑", stockCodeToCorpCode.size());

        List<Stock> stocksToUpdate = stockRepository.findByDartCorpCodeIsNull();
        int updatedCount = 0;

        for (Stock stock : stocksToUpdate) {
            String corpCode = stockCodeToCorpCode.get(stock.getSymbol());
            if (corpCode != null) {
                stock.updateDartCorpCode(corpCode);
                updatedCount++;
            } else {
                log.warn("[StockCorpCodeInitializer] dart_corp_code 매핑 실패: symbol={}, name={}", stock.getSymbol(), stock.getName());
            }
        }

        stockRepository.saveAll(stocksToUpdate);
        log.info("[StockCorpCodeInitializer] dart_corp_code 초기화 완료: {}건 업데이트", updatedCount);
    }

    private Map<String, String> parseCorpCodeZip(byte[] zipBytes) {
        Map<String, String> result = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("CORPCODE.xml".equals(entry.getName())) {
                    result = parseCorpCodeXml(zis);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("[StockCorpCodeInitializer] ZIP 파싱 실패: {}", e.getMessage());
        }
        return result;
    }

    private Map<String, String> parseCorpCodeXml(InputStream is) {
        Map<String, String> result = new HashMap<>();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            // XXE 방지
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            XMLStreamReader reader = factory.createXMLStreamReader(is);
            String currentElement = null;
            String corpCode = null;
            String stockCode = null;

            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        currentElement = reader.getLocalName();
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        String text = reader.getText().trim();
                        if (text.isEmpty()) break;
                        if ("corp_code".equals(currentElement)) corpCode = text;
                        else if ("stock_code".equals(currentElement)) stockCode = text;
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if ("list".equals(reader.getLocalName())) {
                            if (stockCode != null && !stockCode.isBlank() && corpCode != null) {
                                result.put(stockCode, corpCode);
                            }
                            corpCode = null;
                            stockCode = null;
                        }
                        currentElement = null;
                        break;
                    default:
                        break;
                }
            }
            reader.close();
        } catch (Exception e) {
            log.error("[StockCorpCodeInitializer] XML 파싱 실패: {}", e.getMessage());
        }
        return result;
    }
}
