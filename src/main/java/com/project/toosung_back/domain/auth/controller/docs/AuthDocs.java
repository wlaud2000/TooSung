package com.project.toosung_back.domain.auth.controller.docs;

import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth API", description = "인증 관련 API")
public interface AuthDocs {

    @Operation(
            summary = "회원 가입",
            description = "신규 회원을 등록합니다. 이메일 중복 검증 후 회원 정보를 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "회원 가입 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "COMMON-400",
                                    "message": "닉네임은 2자 이상 10자 이하로 입력해주세요."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이메일 중복",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "AUTH-001",
                                    "message": "이미 사용 중인 이메일입니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<AuthResDTO.ResSignUp> signUp(
            @RequestBody @Valid AuthReqDTO.ReqSignUp reqDTO
    );
}
