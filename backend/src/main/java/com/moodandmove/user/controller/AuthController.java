package com.moodandmove.user.controller;

import com.moodandmove.common.security.JwtProvider;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.dto.request.LoginRequest;
import com.moodandmove.user.dto.request.SignupRequest;
import com.moodandmove.user.dto.response.EmailCheckResponse;
import com.moodandmove.user.dto.response.LoginResponse;
import com.moodandmove.user.dto.response.MeResponse;
import com.moodandmove.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        authService.signup(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(
            @RequestParam String email
    ) {
        boolean duplicate = authService.checkEmailDuplicate(email);

        return ResponseEntity.ok(
                new EmailCheckResponse(!duplicate)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        User user = authService.login(request);

        boolean withdrawalPending =
                authService.isWithdrawalPending(user.getId());

        if (withdrawalPending) {
            return ResponseEntity.ok(
                    new LoginResponse(true)
            );
        }

        String accessToken =
                jwtProvider.createAccessToken(user);

        String refreshToken =
                jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt =
                LocalDateTime.ofInstant(
                        jwtProvider
                                .getExpiration(refreshToken)
                                .toInstant(),
                        ZoneId.systemDefault()
                );

        authService.saveRefreshToken(
                user,
                refreshToken,
                refreshExpiresAt
        );

        ResponseCookie accessCookie =
                ResponseCookie
                        .from(
                                "accessToken",
                                accessToken
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMinutes(30)
                        )
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                "refreshToken",
                                refreshToken
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider
                                                .getRefreshTokenExpirationMillis()
                                )
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(
                        new LoginResponse(false)
                );
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        MeResponse response = new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAgeGroup(),
                user.getGender(),
                user.isOnboardingCompleted()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        authService.logout(user.getId());

        ResponseCookie accessCookie =
                ResponseCookie
                        .from(
                                "accessToken",
                                ""
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();


        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                "refreshToken",
                                ""
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .build();
    }

    @PostMapping("/recover")
    public ResponseEntity<Void> recoverAccount(
            @Valid @RequestBody LoginRequest request
    ) {

        User user = authService.recoverAccount(request);

        String accessToken = jwtProvider.createAccessToken(user);

        String refreshToken = jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt =
                LocalDateTime.ofInstant(
                        jwtProvider
                                .getExpiration(refreshToken)
                                .toInstant(),
                        ZoneId.systemDefault()
                );


        authService.saveRefreshToken(
                user,
                refreshToken,
                refreshExpiresAt
        );


        ResponseCookie accessCookie =
                ResponseCookie
                        .from(
                                "accessToken",
                                accessToken
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMinutes(30)
                        )
                        .build();


        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                "refreshToken",
                                refreshToken
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider
                                                .getRefreshTokenExpirationMillis()
                                )
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(
                    name = "refreshToken",
                    required = false
            ) String refreshToken
    ) {

        User user =
                authService.validateRefreshToken(
                        refreshToken
                );

        String newAccessToken = jwtProvider.createAccessToken(user);

        String newRefreshToken = jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt =
                LocalDateTime.ofInstant(
                        jwtProvider
                                .getExpiration(newRefreshToken)
                                .toInstant(),
                        ZoneId.systemDefault()
                );

        authService.saveRefreshToken(
                user,
                newRefreshToken,
                refreshExpiresAt
        );

        ResponseCookie accessCookie =
                ResponseCookie
                        .from(
                                "accessToken",
                                newAccessToken
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMinutes(30)
                        )
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                "refreshToken",
                                newRefreshToken
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider
                                                .getRefreshTokenExpirationMillis()
                                )
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .build();
    }
}