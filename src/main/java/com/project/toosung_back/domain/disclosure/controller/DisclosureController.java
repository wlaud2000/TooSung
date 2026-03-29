package com.project.toosung_back.domain.disclosure.controller;

import com.project.toosung_back.domain.disclosure.controller.docs.DisclosureDocs;
import com.project.toosung_back.domain.disclosure.dto.response.DisclosureResDTO;
import com.project.toosung_back.domain.disclosure.service.query.DisclosureQueryService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/disclosures")
public class DisclosureController implements DisclosureDocs {

    private final DisclosureQueryService disclosureQueryService;

    @Override
    @GetMapping("/{disclosureId}")
    public CustomResponse<DisclosureResDTO.DisclosureDetail> getDisclosure(
            @PathVariable Long disclosureId
    ) {
        DisclosureResDTO.DisclosureDetail dto = disclosureQueryService.getDisclosure(disclosureId);
        return CustomResponse.onSuccess("공시 상세 조회 성공", dto);
    }

    @Override
    @GetMapping
    public CustomResponse<DisclosureResDTO.DisclosureList> getDisclosures(
            @RequestParam Long stockId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int size
    ) {
        DisclosureResDTO.DisclosureList dto = disclosureQueryService.getDisclosures(stockId, cursor, type, size);
        return CustomResponse.onSuccess("공시 목록 조회 성공", dto);
    }
}