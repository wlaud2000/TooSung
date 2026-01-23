package com.project.toosung_back.global.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class S3DTO {

    @Schema(description = "Pre-Signed URL 요청")
    public record PresignedUrlRequest(
            @Schema(description = "파일명 (확장자 포함)", example = "profile.jpg")
            @NotBlank(message = "파일명은 필수입니다.")
            String fileName
    ) {}

    @Schema(description = "Pre-Signed URL 응답")
    @Builder
    public record PresignedUrlResponse(
            @Schema(description = "S3 업로드용 Pre-Signed URL (PUT 요청용)")
            String presignedUrl,

            @Schema(description = "업로드 완료 후 접근 가능한 파일 URL")
            String fileUrl
    ) {}
}
