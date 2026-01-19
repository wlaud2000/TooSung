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
     * OAuth 콜백 처리
     * GET /api/v1/oauth2/{provider}/callback
     */
    @Override
    @GetMapping("/oauth2/callback/{provider}")
    public CustomResponse<OAuthResDTO.LoginResponse> handleCallback(
            @PathVariable("provider") Provider provider,
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpSession session
    ) {
        OAuthResDTO.LoginResponse resDTO = oAuthService.handleCallback(provider, code, state, session);
        return CustomResponse.onSuccess(HttpStatus.OK, provider.name() + " 로그인 성공", resDTO);
    }
}
