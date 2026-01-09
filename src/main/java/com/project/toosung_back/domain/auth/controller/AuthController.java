package com.project.toosung_back.domain.auth.controller;

import com.project.toosung_back.domain.auth.controller.docs.AuthDocs;
import com.project.toosung_back.domain.auth.dto.request.AuthReqDTO;
import com.project.toosung_back.domain.auth.dto.response.AuthResDTO;
import com.project.toosung_back.domain.auth.service.AuthService;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthDocs {

    private final AuthService authService;

    @Override
    @PostMapping("/signup")
    public CustomResponse<AuthResDTO.ResSignUp> signUp(@RequestBody @Valid AuthReqDTO.ReqSignUp reqDTO) {
        AuthResDTO.ResSignUp resDTO = authService.signUp(reqDTO);
        return CustomResponse.onSuccess(HttpStatus.CREATED, "회원 가입이 완료되었습니다.", resDTO);
    }
}
