package com.project.toosung_back.domain.disclosure.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.domain.disclosure.converter.DisclosureConverter;
import com.project.toosung_back.domain.disclosure.dto.response.DisclosureResDTO;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.disclosure.exception.DisclosureErrorCode;
import com.project.toosung_back.domain.disclosure.exception.DisclosureException;
import com.project.toosung_back.domain.disclosure.repository.DisclosureAnalysisRepository;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureQueryService {

    private static final String CACHE_PREFIX = "disclosure:list:";
    private static final long CACHE_TTL_SECONDS = 3600L;

    private final DisclosureRepository disclosureRepository;
    private final DisclosureAnalysisRepository disclosureAnalysisRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DisclosureResDTO.DisclosureDetail getDisclosure(Long disclosureId) {
        Disclosure disclosure = disclosureRepository.findByIdWithStock(disclosureId)
                .orElseThrow(() -> new DisclosureException(DisclosureErrorCode.DISCLOSURE_NOT_FOUND));

        Optional<DisclosureAnalysis> analysis = disclosureAnalysisRepository.findByDisclosureId(disclosureId);

        return DisclosureConverter.toDisclosureDetail(disclosure, analysis);
    }

    @Transactional(readOnly = true)
    public DisclosureResDTO.DisclosureList getDisclosures(Long stockId, Long cursor, String type, int size) {
        String cacheKey = CACHE_PREFIX + stockId + ":cursor:" + cursor + ":type:" + type + ":size:" + size;

        if (redisUtil.hasKey(cacheKey)) {
            String cached = redisUtil.get(cacheKey);
            try {
                return objectMapper.readValue(cached, DisclosureResDTO.DisclosureList.class);
            } catch (JsonProcessingException e) {
                log.warn("[DisclosureQueryService] 캐시 역직렬화 실패, 재조회 - key={}", cacheKey);
                redisUtil.delete(cacheKey);
            }
        }

        Slice<Disclosure> slice = disclosureRepository.findDisclosures(
                stockId, cursor, type, PageRequest.of(0, size));

        List<DisclosureResDTO.DisclosureItem> items = slice.getContent().stream()
                .map(DisclosureConverter::toDisclosureItem)
                .toList();

        Long nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getId()
                : null;

        DisclosureResDTO.DisclosureList result = DisclosureResDTO.DisclosureList.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();

        try {
            redisUtil.save(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("[DisclosureQueryService] 캐시 저장 실패 - key={}", cacheKey);
        }

        return result;
    }
}
