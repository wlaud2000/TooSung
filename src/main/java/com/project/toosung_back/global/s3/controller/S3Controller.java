package com.project.toosung_back.global.s3.controller;

import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.s3.service.S3UploadService;
import com.project.toosung_back.global.s3.controller.docs.S3Docs;
import com.project.toosung_back.global.s3.dto.S3DTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/s3")
public class S3Controller implements S3Docs {

    private final S3UploadService s3UploadService;

    @Override
    @PostMapping("/presigned-url/profile")
    public CustomResponse<S3DTO.PresignedUrlResponse> getProfileImageUploadUrl(
            @RequestBody @Valid S3DTO.PresignedUrlRequest request
    ) {
        S3DTO.PresignedUrlResponse response = s3UploadService.generatePresignedUrl("profile", request.fileName());
        return CustomResponse.onSuccess("성공", response);
    }
}
