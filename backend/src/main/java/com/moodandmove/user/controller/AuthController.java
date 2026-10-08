package com.moodandmove.user.controller;

import com.moodandmove.common.security.JwtProvider;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.dto.request.LoginRequest;
import com.moodandmove.user.dto.request.SignupRequest;
import com.moodandmove.user.dto.response.EmailCheckResponse;
import com.moodandmove.user.dto.response.LoginResponse;
import com.moodandmove.user.dto.response.MeResponse;
import com.moodandmove.user.service.AuthService;
import com.moodandmove.user.service.KakaoOAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.moodandmove.user.dto.response.KakaoUserResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final KakaoOAuthService kakaoOAuthService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {

        authService.signup(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(@RequestParam String email) {

        boolean duplicate = authService.checkEmailDuplicate(email);

        return ResponseEntity.ok(new EmailCheckResponse(!duplicate));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        User user = authService.login(request);

        boolean withdrawalPending = authService.isWithdrawalPending(user.getId());

        if (withdrawalPending) {
            return ResponseEntity.ok(new LoginResponse(true));
        }

        user = authService.startLoginSession(user.getId());

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
                        .from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis()))
                        .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(false));
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
                user.isOnboardingCompleted(),
                user.isLocalLoginEnabled()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        authService.logout(user.getId());

        ResponseCookie accessCookie =
                ResponseCookie
                        .from("accessToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();


        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @PostMapping("/recover")
    public ResponseEntity<Void> recoverAccount(@Valid @RequestBody LoginRequest request) {
        User user = authService.recoverAccount(request);

        String accessToken = jwtProvider.createAccessToken(user);

        String refreshToken = jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt = LocalDateTime.ofInstant(
                jwtProvider.getExpiration(refreshToken)
                        .toInstant(),
                        ZoneId.systemDefault()
                );


        authService.saveRefreshToken(user, refreshToken, refreshExpiresAt);


        ResponseCookie accessCookie =
                ResponseCookie
                        .from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();


        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis()))
                        .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(
                    name = "refreshToken",
                    required = false
            ) String refreshToken
    ) {

        User user = authService.validateRefreshToken(refreshToken);

        String newAccessToken = jwtProvider.createAccessToken(user);

        String newRefreshToken = jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt =
                LocalDateTime.ofInstant(
                        jwtProvider
                                .getExpiration(newRefreshToken)
                                .toInstant(),
                        ZoneId.systemDefault()
                );

        authService.saveRefreshToken(user, newRefreshToken, refreshExpiresAt);

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
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @PostMapping("/social/recover")
    public ResponseEntity<Void> recoverSocialAccount(
            @CookieValue(
                    name = "recoveryToken",
                    required = false
            )String recoveryToken
    ){
        User user = authService.validateRecoveryToken(recoveryToken);

        authService.recoverSocialAccount(user.getId());

        user = authService.startLoginSession(user.getId());

        String accessToken = jwtProvider.createAccessToken(user);

        String refreshToken = jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt = LocalDateTime.ofInstant(jwtProvider
                                .getExpiration(refreshToken)
                                .toInstant(),
                        ZoneId.systemDefault()
                );

        authService.saveRefreshToken(user, refreshToken, refreshExpiresAt);
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

        ResponseCookie refreshCookie = ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis())
                        )
                        .build();

        ResponseCookie recoveryCookie = ResponseCookie
                        .from("recoveryToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, recoveryCookie.toString())
                .build();
    }

    @GetMapping("/kakao/login")
    public void kakaoLogin(HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(kakaoOAuthService.getAuthorizationUrl());
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(@RequestParam String code) {
        String kakaoAccessToken = kakaoOAuthService.getAccessToken(code);

        KakaoUserResponse kakaoUser = kakaoOAuthService.getUser(kakaoAccessToken);

        KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();

        if (account == null || account.email() == null ||
                !Boolean.TRUE.equals(account.emailValid()) ||
                !Boolean.TRUE.equals(account.emailVerified())) {
            throw new IllegalArgumentException(
                    "카카오 이메일 정보를 확인할 수 없습니다."
            );
        }

        String nickname = account.profile() != null && account.profile().nickname() != null
                        ? account.profile().nickname() : "Mood&Move 사용자";

        User user = authService.findOrCreateKakaoUser(kakaoUser.id(), account.email(), nickname);

        if (authService.isWithdrawalPending(user.getId())) {

            String recoveryToken = jwtProvider.createRecoveryToken(user);

            ResponseCookie recoveryCookie = ResponseCookie
                            .from("recoveryToken", recoveryToken)
                            .httpOnly(true)
                            .secure(false)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(Duration.ofMillis(jwtProvider.getRecoveryTokenExpirationMillis()))
                            .build();

            ResponseCookie accessCookie = ResponseCookie
                            .from("accessToken", "")
                            .httpOnly(true)
                            .secure(false)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(0)
                            .build();

            ResponseCookie refreshCookie = ResponseCookie
                            .from("refreshToken", "")
                            .httpOnly(true)
                            .secure(false)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(0)
                            .build();

            return ResponseEntity
                    .status(302)
                    .header(HttpHeaders.SET_COOKIE, recoveryCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.LOCATION, "http://localhost:5173/account-recovery")
                    .build();
        }

        user = authService.startLoginSession(user.getId());

        String accessToken = jwtProvider.createAccessToken(user);

        String refreshToken = jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt = LocalDateTime.ofInstant(jwtProvider.getExpiration(refreshToken)
                                .toInstant(), ZoneId.systemDefault());

        authService.saveRefreshToken(user, refreshToken, refreshExpiresAt);

        ResponseCookie accessCookie = ResponseCookie
                        .from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis()))
                        .build();

        return ResponseEntity
                .status(302)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.LOCATION, "http://localhost:5173/")
                .build();
    }

    @PostMapping("/social/recover/cancel")
    public ResponseEntity<Void> cancelSocialRecovery() {

        ResponseCookie recoveryCookie =
                ResponseCookie
                        .from("recoveryToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, recoveryCookie.toString())
                .build();
    }
}