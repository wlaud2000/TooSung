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
import org.springframework.web.multipart.MultipartFile;

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
                    - 프로필 이미지 (jpeg, png, gif, webp / 최대 5MB)

                    **요청 형식:** multipart/form-data
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
                            examples = {
                                    @ExampleObject(name = "유효성 검증 실패", value = """
                                        {
                                            "isSuccess": false,
                                            "code": "COMMON-400",
                                            "message": "닉네임은 2자 이상 10자 이하로 입력해주세요."
                                        }
                                        """),
                                    @ExampleObject(name = "파일 형식 오류", value = """
                                        {
                                            "isSuccess": false,
                                            "code": "S3-003",
                                            "message": "지원하지 않는 파일 형식입니다. (jpeg, png, gif, webp만 가능)"
                                        }
                                        """),
                                    @ExampleObject(name = "파일 크기 초과", value = """
                                        {
                                            "isSuccess": false,
                                            "code": "S3-002",
                                            "message": "파일 크기는 5MB를 초과할 수 없습니다."
                                        }
                                        """)
                            }
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
            @Parameter(description = "수정할 회원 정보 (닉네임)") MemberReqDTO.ReqUpdateProfile reqDTO,
            @Parameter(description = "프로필 이미지 파일") MultipartFile profileImage
    );
}
