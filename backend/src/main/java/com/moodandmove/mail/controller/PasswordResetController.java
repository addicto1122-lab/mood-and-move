
package com.moodandmove.mail.controller;

import com.moodandmove.mail.dto.request.PasswordResetSendRequest;
import com.moodandmove.mail.dto.request.PasswordResetVerifyRequest;
import com.moodandmove.mail.dto.request.PasswordResetConfirmRequest;
import com.moodandmove.mail.service.EmailSendRateLimiter;
import com.moodandmove.mail.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final EmailSendRateLimiter rateLimiter;

    // 1. 비밀번호 재설정 인증번호 발송
    @PostMapping("/send")
    public ResponseEntity<?> sendCode(@Valid @RequestBody PasswordResetSendRequest request, HttpServletRequest httpRequest) {

        try {
            String clientIp = httpRequest.getRemoteAddr();

            // 기존 회원가입 이메일 발송 제한 기능 재사용
            rateLimiter.check(clientIp, request.email());

            passwordResetService.sendCode(request.email());

            // 계정 존재 여부를 외부에 노출하지 않음
            return ResponseEntity.ok(
                    Map.of("message", "등록된 계정이라면 인증번호가 발송됩니다.")
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    // 2. 인증번호 검증
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody PasswordResetVerifyRequest request) {

        PasswordResetService.VerifyResult result =
                passwordResetService.verifyCode(request.email(), request.code());

        if (!result.success()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", result.message()));
        }

        return ResponseEntity.ok(
                Map.of("message", result.message(), "resetToken", result.resetToken())
        );
    }

    // 3. 새 비밀번호 설정
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {

        passwordResetService.resetPassword(request.resetToken(), request.newPassword());

        return ResponseEntity.ok(
                Map.of("message", "비밀번호가 변경되었습니다. 다시 로그인해주세요.")
        );
    }
}
