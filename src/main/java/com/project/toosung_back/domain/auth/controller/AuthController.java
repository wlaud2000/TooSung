package com.project.toosung_back.domain.auth.controller;

import com.project.toosung_back.domain.auth.controller.docs.AuthDocs;
import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.request.OAuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.dto.response.OAuthResDTO;
import com.project.toosung_back.domain.auth.service.AuthService;
import com.project.toosung_back.domain.auth.service.oauth.OAuthService;
import com.project.toosung_back.domain.auth.service.oauth.kakao.KakaoOAuthService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthDocs {

    private final AuthService authService;
    private final OAuthService oAuthService;

    @Override
    @PostMapping("/signup")
    public CustomResponse<AuthResDTO.ResSignUp> signUp(@RequestBody @Valid AuthReqDTO.ReqSignUp reqDTO) {
        AuthResDTO.ResSignUp resDTO = authService.signUp(reqDTO);
        return CustomResponse.onSuccess(HttpStatus.CREATED, "회원 가입이 완료되었습니다.", resDTO);
    }

    @Override
    @PostMapping("/kakao")
    public CustomResponse<OAuthResDTO.LoginResponse> kakaoLogin(@RequestBody OAuthReqDTO.OAuthLoginRequest reqDTO) {
        OAuthResDTO.LoginResponse resDTO = oAuthService.kakaoLogin(reqDTO.code());
        return CustomResponse.onSuccess(HttpStatus.OK, "카카오 로그인 성공", resDTO);
    }

    /**
     * Swagger 문서용 가짜 로그인 엔드포인트
     * 실제 처리는 CustomLoginFilter에서 수행
     */
    @Override
    @PostMapping("/login")
    public void login(@RequestBody @Valid AuthReqDTO.Login reqDTO) {
        // 실제로 호출되지 않음 - CustomLoginFilter가 처리
        throw new IllegalStateException("이 메서드는 호출되면 안됩니다.");
    }

    /**
     * Swagger 문서용 가짜 로그아웃 엔드포인트
     * 실제 처리는 LogoutFilter + CustomLogoutHandler에서 수행
     */
    @Override
    @PostMapping("/logout")
    public void logout() {
        // 실제로 호출되지 않음 - Spring Security LogoutFilter가 처리
        throw new IllegalStateException("이 메서드는 호출되면 안됩니다.");
    }
}
