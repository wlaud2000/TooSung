package com.project.toosung_back.domain.auth.controller.docs;

import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

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

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다. 성공 시 JWT 토큰이 쿠키에 저장됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "isSuccess": true,
                                "code": "COMMON-200",
                                "message": "성공입니다.",
                                "data": {
                                    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                }
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "로그인 실패",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "isSuccess": false,
                                "code": "SEC-004",
                                "message": "이메일 또는 비밀번호가 올바르지 않습니다."
                            }
                            """)
                    )
            )
    })
    void login(@RequestBody @Valid AuthReqDTO.Login reqDTO);

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃합니다. 토큰이 블랙리스트에 등록되고 쿠키가 삭제됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공"
            )
    })
    void logout();

    @Operation(
            summary = "소셜 로그인 페이지 리다이렉트",
            description = """
                    소셜 로그인 페이지로 리다이렉트합니다.

                    **지원 Provider:** KAKAO, GOOGLE

                    **흐름:**
                    1. 클라이언트가 이 엔드포인트 호출
                    2. 서버가 CSRF 방지용 state 생성 후 세션에 저장
                    3. 해당 Provider의 로그인 페이지로 리다이렉트
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "소셜 로그인 페이지로 리다이렉트"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 Provider",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "isSuccess": false,
                                "code": "OAUTH_004",
                                "message": "지원하지 않는 소셜 로그인입니다."
                            }
                            """)
                    )
            )
    })
    void redirectToProvider(
            @Parameter(description = "OAuth Provider (KAKAO, GOOGLE)", required = true)
            @PathVariable("provider") Provider provider,
            HttpServletResponse response,
            HttpSession session
    ) throws IOException;

    @Operation(
            summary = "소셜 로그인 콜백 처리",
            description = """
                    OAuth Provider로부터 콜백을 처리합니다.

                    **흐름:**
                    1. Provider가 인가 코드와 state를 전달하며 리다이렉트
                    2. 서버가 state 검증 (CSRF 방지)
                    3. 인가 코드로 액세스 토큰 발급
                    4. 액세스 토큰으로 사용자 정보 조회
                    5. 회원 조회 또는 신규 가입 처리
                    6. JWT 토큰 발급 후 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "소셜 로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "isSuccess": true,
                                "code": "200",
                                "message": "KAKAO 로그인 성공",
                                "result": {
                                    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                    "memberInfo": {
                                        "id": 1,
                                        "email": "user@example.com",
                                        "nickname": "홍길동",
                                        "profileImageUrl": "https://example.com/profile.jpg"
                                    }
                                }
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 state 또는 인가 코드",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "isSuccess": false,
                                "code": "OAUTH_005",
                                "message": "유효하지 않은 state 값입니다."
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "소셜 로그인 서버 통신 실패",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                                "isSuccess": false,
                                "code": "OAUTH_002",
                                "message": "소셜 로그인 토큰 발급에 실패했습니다."
                            }
                            """)
                    )
            )
    })
    CustomResponse<OAuthResDTO.LoginResponse> handleCallback(
            @Parameter(description = "OAuth Provider (KAKAO, GOOGLE)", required = true)
            @PathVariable("provider") Provider provider,
            @Parameter(description = "인가 코드", required = true)
            @RequestParam("code") String code,
            @Parameter(description = "CSRF 방지용 state", required = true)
            @RequestParam("state") String state,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException;
}
