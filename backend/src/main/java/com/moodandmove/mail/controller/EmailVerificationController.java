package com.moodandmove.mail.controller;

import com.moodandmove.mail.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    public record EmailSendRequest(String email) { }

    // 이메일 인증번호 발송
    @PostMapping("/send")
    public ResponseEntity<?> sendCode(
            @RequestBody EmailSendRequest request
    ) {
        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "이메일을 입력해주세요."));
        }

        try {
            emailVerificationService.sendCode(request.email());

            return ResponseEntity.ok(
                    Map.of("message", "인증번호를 발송했습니다.")
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(
            @RequestBody EmailVerifyRequest request
    ) {
        EmailVerificationService.VerifyResult result =
                emailVerificationService.verifyCode(
                        request.email(),
                        request.code()
                );

        if (!result.success()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", result.message())
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", result.message(),
                        "signupToken", result.signupToken()
                )
        );
    }

    public record EmailVerifyRequest(
            String email,
            String code
    ) {
    }
}