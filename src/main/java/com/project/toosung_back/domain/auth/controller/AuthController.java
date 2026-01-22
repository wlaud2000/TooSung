package com.project.toosung_back.domain.auth.controller;

import com.project.toosung_back.domain.auth.controller.docs.AuthDocs;
import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.domain.auth.service.AuthService;
import com.project.toosung_back.domain.auth.service.oauth.OAuthService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * OAuth 소셜 로그인 통합 컨트롤러
 * GET /api/v1/oauth2/{provider} - 로그인 페이지 리다이렉트
 * GET /api/v1/oauth2/{provider}/callback - 콜백 처리
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController implements AuthDocs {

    private final AuthService authService;
    private final OAuthService oAuthService;

    @Override
    @PostMapping("/auth/signup")
    public CustomResponse<AuthResDTO.ResSignUp> signUp(@RequestBody @Valid AuthReqDTO.ReqSignUp reqDTO) {
        AuthResDTO.ResSignUp resDTO = authService.signUp(reqDTO);
        return CustomResponse.onSuccess(HttpStatus.CREATED, "회원 가입이 완료되었습니다.", resDTO);
    }

    /**
     * Swagger 문서용 가짜 로그인 엔드포인트
     * 실제 처리는 CustomLoginFilter에서 수행
     */
    @Override
    @PostMapping("/auth/login")
    public void login(@RequestBody @Valid AuthReqDTO.Login reqDTO) {
        throw new IllegalStateException("이 메서드는 호출되면 안됩니다.");
    }

    /**
     * Swagger 문서용 가짜 로그아웃 엔드포인트
     * 실제 처리는 LogoutFilter + CustomLogoutHandler에서 수행
     */
    @Override
    @PostMapping("/auth/logout")
    public void logout() {
        throw new IllegalStateException("이 메서드는 호출되면 안됩니다.");
    }

    /**
     * OAuth 소셜 로그인 페이지로 리다이렉트
     * GET /api/v1/oauth2/{provider} (예: /api/v1/oauth2/kakao, /api/v1/oauth2/google)
     */
    @Override
    @GetMapping("/oauth2/{provider}")
    public void redirectToProvider(
            @PathVariable("provider") Provider provider,
            HttpServletResponse response,
            HttpSession session
    ) throws IOException {
        oAuthService.redirectToProvider(provider, response, session);
    }

    /**
     * OAuth 콜백 처리 및 리다이렉트
     * GET /api/v1/oauth2/callback/{provider}
     *
     * 변경사항:
     * 1. 반환 타입: CustomResponse -> void (리다이렉트 하므로 웅답 바디 없음)
     * 2. 로직: 로그인 처리 후 JSON 반환 대신 프론트엔드 URL로 리다이렉트 수행
     */
    @GetMapping("/oauth2/callback/{provider}")
    public CustomResponse<OAuthResDTO.LoginResponse> handleCallback(
            @PathVariable("provider") Provider provider, // Enum 변환 문제 방지를 위해 String 권장, 필요시 Provider로 유지
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpSession session, // 필요 없다면 제거 가능
            HttpServletResponse response
    ) throws IOException {
        try {
            // 1. 서비스 로직 수행 (여기서 토큰 발급 및 쿠키 설정이 내부적으로 이루어진다고 가정)
            OAuthResDTO.LoginResponse resDTO = oAuthService.handleCallback(provider, code, state, session, response);
            // 2. 성공 시 대시보드로 이동
            response.sendRedirect("http://localhost:5173/dashboard");
            return CustomResponse.onSuccess("로그인 성공", resDTO);
        } catch (Exception e) {
            e.printStackTrace();
            // 3. 실패 시 로그인 페이지로 이동 (에러 메시지 전달)
            response.sendRedirect("http://localhost:5173/auth/login?error=social_login_failed");
            return CustomResponse.onFailure("failure","소셜 로그인 실패");
        }
    }
}
