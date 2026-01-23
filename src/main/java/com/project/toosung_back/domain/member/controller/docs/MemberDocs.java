package com.project.toosung_back.domain.member.controller.docs;

import com.project.toosung_back.domain.member.dto.request.MemberReqDTO;
import com.project.toosung_back.domain.member.dto.response.MemberResDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import com.project.toosung_back.global.security.userdetails.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member API", description = "회원 관련 API")
public interface MemberDocs {

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 회원의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "COMMON-200",
                                    "message": "성공입니다.",
                                    "data": {
                                        "email": "user@example.com",
                                        "nickname": "홍길동",
                                        "profileImageUrl": "https://example.com/profile.jpg",
                                        "isSocialLogin": true
                                    }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "SEC-001",
                                    "message": "인증이 필요합니다."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "MEMBER-001",
                                    "message": "존재하지 않는 회원입니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<MemberResDTO.ResMemberInfo> getMyInfo(@Parameter(hidden = true) AuthUser authUser);

    @Operation(
            summary = "내 정보 수정",
            description = """
                    현재 로그인한 회원의 정보를 수정합니다.

                    **수정 가능 항목:**
                    - 닉네임 (2~10자)
                    - 프로필 이미지 URL (S3 Pre-Signed URL로 업로드 후 받은 fileUrl)

                    **프로필 이미지 수정 흐름:**
                    1. `POST /api/v1/s3/presigned-url/profile` 호출하여 업로드 URL 발급
                    2. 발급받은 presignedUrl로 PUT 요청하여 S3에 직접 업로드
                    3. 이 API에 fileUrl 전달하여 프로필 정보 업데이트
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "COMMON-200",
                                    "message": "회원 정보 수정 성공",
                                    "data": {
                                        "email": "user@example.com",
                                        "nickname": "새닉네임",
                                        "profileImageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/profile/uuid.jpg",
                                        "isSocialLogin": true
                                    }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(name = "유효성 검증 실패", value = """
                                {
                                    "isSuccess": false,
                                    "code": "COMMON-400",
                                    "message": "닉네임은 2자 이상 10자 이하로 입력해주세요."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "SEC-001",
                                    "message": "인증이 필요합니다."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "MEMBER-001",
                                    "message": "존재하지 않는 회원입니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<MemberResDTO.ResMemberInfo> updateProfile(
            @Parameter(hidden = true) AuthUser authUser,
            MemberReqDTO.ReqUpdateProfile reqDTO
    );

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    현재 로그인한 회원을 탈퇴 처리합니다.

                    **Soft Delete 방식:**
                    - 실제 데이터는 삭제되지 않고 `deletedAt` 필드에 탈퇴 시각이 기록됩니다.
                    - 탈퇴한 회원은 로그인 및 서비스 이용이 불가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "COMMON-200",
                                    "message": "회원 탈퇴 성공",
                                    "data": null
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 탈퇴한 회원",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "MEMBER-002",
                                    "message": "이미 탈퇴한 회원입니다."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "SEC-001",
                                    "message": "인증이 필요합니다."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "MEMBER-001",
                                    "message": "존재하지 않는 회원입니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<Void> withdraw(@Parameter(hidden = true) AuthUser authUser);
}
