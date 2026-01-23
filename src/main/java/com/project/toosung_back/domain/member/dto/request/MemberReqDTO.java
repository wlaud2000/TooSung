package com.project.toosung_back.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public class MemberReqDTO {

    @Schema(description = "회원 정보 수정 요청")
    public record ReqUpdateProfile(
            @Schema(description = "닉네임 (2~10자)", example = "홍길동")
            @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
            String nickname,

            @Schema(description = "프로필 이미지 URL (S3 업로드 후 받은 fileUrl)", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/profile/uuid.jpg")
            String profileImageUrl
    ) {}
}
