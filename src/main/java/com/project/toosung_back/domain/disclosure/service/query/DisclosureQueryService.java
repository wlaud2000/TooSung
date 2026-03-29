package com.project.toosung_back.domain.disclosure.service.query;

import com.project.toosung_back.domain.disclosure.converter.DisclosureConverter;
import com.project.toosung_back.domain.disclosure.dto.response.DisclosureResDTO;
import com.project.toosung_back.domain.disclosure.exception.DisclosureErrorCode;
import com.project.toosung_back.domain.disclosure.exception.DisclosureException;
import com.project.toosung_back.domain.disclosure.repository.DisclosureRepository;
import com.project.toosung_back.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DisclosureQueryService {

    private final DisclosureRepository disclosureRepository;

    @Transactional(readOnly = true)
    public DisclosureResDTO.DisclosureDetail getDisclosure(Long disclosureId) {
        return disclosureRepository.findByIdWithStock(disclosureId)
                .map(DisclosureConverter::toDisclosureDetail)
                .orElseThrow(() -> new DisclosureException(DisclosureErrorCode.DISCLOSURE_NOT_FOUND));
    }
}
