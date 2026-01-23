package com.project.toosung_back.domain.member.controller.docs;

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
}
