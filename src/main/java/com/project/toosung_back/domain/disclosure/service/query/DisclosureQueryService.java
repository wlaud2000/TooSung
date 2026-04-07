package com.project.toosung_back.domain.disclosure.service.query;

import com.project.toosung_back.domain.disclosure.converter.DisclosureConverter;
import com.project.toosung_back.domain.disclosure.dto.response.DisclosureResDTO;
import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import com.project.toosung_back.domain.disclosure.exception.DisclosureErrorCode;
import com.project.toosung_back.domain.disclosure.exception.DisclosureException;
import com.project.toosung_back.domain.disclosure.repository.DisclosureAnalysisRepository;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DisclosureQueryService {

    private final DisclosureRepository disclosureRepository;
    private final DisclosureAnalysisRepository disclosureAnalysisRepository;

    @Transactional(readOnly = true)
    public DisclosureResDTO.DisclosureDetail getDisclosure(Long disclosureId) {
        Disclosure disclosure = disclosureRepository.findByIdWithStock(disclosureId)
                .orElseThrow(() -> new DisclosureException(DisclosureErrorCode.DISCLOSURE_NOT_FOUND));

        Optional<DisclosureAnalysis> analysis = disclosureAnalysisRepository.findByDisclosureId(disclosureId);

        return DisclosureConverter.toDisclosureDetail(disclosure, analysis);
    }

    @Cacheable(cacheNames = "disclosure", key = "#stockId + ':cursor:' + #cursor + ':type:' + #type + ':size:' + #size")
    @Transactional(readOnly = true)
    public DisclosureResDTO.DisclosureList getDisclosures(Long stockId, Long cursor, String type, int size) {
        Slice<Disclosure> slice = disclosureRepository.findDisclosures(
                stockId, cursor, type, PageRequest.of(0, size));

        List<DisclosureResDTO.DisclosureItem> items = slice.getContent().stream()
                .map(DisclosureConverter::toDisclosureItem)
                .toList();

        Long nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getId()
                : null;

        return DisclosureResDTO.DisclosureList.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();
    }
}
