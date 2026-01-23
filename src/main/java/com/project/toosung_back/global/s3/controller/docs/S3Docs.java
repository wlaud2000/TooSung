package com.project.toosung_back.global.s3.controller.docs;

import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.s3.dto.S3DTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "S3 API", description = "파일 업로드 관련 API")
public interface S3Docs {

    @Operation(
            summary = "프로필 이미지 업로드 URL 발급",
            description = """
                    프로필 이미지 업로드를 위한 Pre-Signed URL을 발급합니다.

                    **사용 방법:**
                    1. 이 API를 호출하여 presignedUrl과 fileUrl을 받습니다.
                    2. presignedUrl로 PUT 요청하여 이미지를 직접 S3에 업로드합니다.
                    3. 업로드 완료 후 fileUrl을 프로필 수정 API에 전달합니다.

                    **지원 형식:** jpeg, jpg, png, gif, webp

                    **URL 유효 시간:** 10분
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "URL 발급 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "COMMON-200",
                                    "message": "성공입니다.",
                                    "data": {
                                        "presignedUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/profile/uuid.jpg?X-Amz-Algorithm=...",
                                        "fileUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/profile/uuid.jpg"
                                    }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 파일 형식",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "S3-003",
                                    "message": "지원하지 않는 파일 형식입니다. (jpeg, png, gif, webp만 가능)"
                                }
                                """)
                    )
            )
    })
    CustomResponse<S3DTO.PresignedUrlResponse> getProfileImageUploadUrl(
            @RequestBody @Valid S3DTO.PresignedUrlRequest request
    );
}
