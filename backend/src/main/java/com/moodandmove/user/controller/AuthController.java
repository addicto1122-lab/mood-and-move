
package com.moodandmove.user.controller;

import com.moodandmove.common.security.JwtProvider;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.dto.request.LoginRequest;
import com.moodandmove.user.dto.request.SignupRequest;
import com.moodandmove.user.dto.response.EmailCheckResponse;
import com.moodandmove.user.dto.response.LoginResponse;
import com.moodandmove.user.dto.response.MeResponse;
import com.moodandmove.user.dto.response.KakaoUserResponse;
import com.moodandmove.user.service.AuthService;
import com.moodandmove.user.service.KakaoOAuthService;
import com.moodandmove.user.service.LoginAttemptLimiter;
import com.moodandmove.common.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final KakaoOAuthService kakaoOAuthService;
    private final LoginAttemptLimiter loginAttemptLimiter;

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

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String clientIp = httpRequest.getRemoteAddr();

        // 1. 로그인 시도 제한 확인
        loginAttemptLimiter.check(
                clientIp,
                request.email()
        );

        User user;

        try {
            // 2. 이메일 및 비밀번호 검증
            user = authService.login(request);

        } catch (InvalidCredentialsException e) {

            // 3. 로그인 실패 기록
            loginAttemptLimiter.recordFailure(
                    clientIp,
                    request.email()
            );

            throw e;
        }

        // 4. 로그인 성공 시 실패 기록 초기화
        loginAttemptLimiter.clear(
                clientIp,
                request.email()
        );

        // 5. 탈퇴 신청 여부 확인
        boolean withdrawalPending =
                authService.isWithdrawalPending(user.getId());

        if (withdrawalPending) {
            return ResponseEntity.ok(
                    new LoginResponse(true)
            );
        }

        // 6. 기존 로그인 세션 처리
        user = authService.startLoginSession(user.getId());

        // 7. JWT 발급
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

        // Access Token 쿠키
        ResponseCookie accessCookie =
                ResponseCookie
                        .from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();

        // Refresh Token 쿠키
        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider.getRefreshTokenExpirationMillis()
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
                .body(new LoginResponse(false));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        MeResponse response = new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAgeGroup(),
                user.getGender(),
                user.isOnboardingCompleted(),
                user.isLocalLoginEnabled(),
                user.getCreatedAt().toLocalDate()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        authService.logout(user.getId());

        ResponseCookie accessCookie =
                ResponseCookie
                        .from("accessToken", "")
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", "")
                        .httpOnly(true)
                        .secure(cookieSecure)
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

    // 탈퇴 신청 계정 복구
    @PostMapping("/recover")
    public ResponseEntity<Void> recoverAccount(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = httpRequest.getRemoteAddr();

        // 계정 복구 시도 제한 확인
        loginAttemptLimiter.check(clientIp, request.email());

        User user;

        try {
            user = authService.recoverAccount(request);

        } catch (InvalidCredentialsException e) {
            loginAttemptLimiter.recordFailure(
                    clientIp,
                    request.email()
            );
            throw e;
        }

        // 복구 성공 시 기록 초기화
        loginAttemptLimiter.clear(clientIp, request.email());


        String accessToken =
                jwtProvider.createAccessToken(user);

        String refreshToken =
                jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt =
                LocalDateTime.ofInstant(
                        jwtProvider.getExpiration(refreshToken)
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
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider.getRefreshTokenExpirationMillis()
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

        AuthService.RotatedTokens tokens =
                authService.rotateRefreshToken(refreshToken);

        String newAccessToken = tokens.accessToken();
        String newRefreshToken = tokens.refreshToken();

        ResponseCookie accessCookie =
                ResponseCookie
                        .from("accessToken", newAccessToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", newRefreshToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider.getRefreshTokenExpirationMillis()
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

    @PostMapping("/social/recover")
    public ResponseEntity<Void> recoverSocialAccount(
            @CookieValue(
                    name = "recoveryToken",
                    required = false
            ) String recoveryToken
    ) {

        User user =
                authService.validateRecoveryToken(recoveryToken);

        authService.recoverSocialAccount(user.getId());

        user = authService.startLoginSession(user.getId());

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
                        .from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider.getRefreshTokenExpirationMillis()
                                )
                        )
                        .build();

        ResponseCookie recoveryCookie =
                ResponseCookie
                        .from("recoveryToken", "")
                        .httpOnly(true)
                        .secure(cookieSecure)
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
                .header(
                        HttpHeaders.SET_COOKIE,
                        recoveryCookie.toString()
                )
                .build();
    }

    @GetMapping("/kakao/login")
    public ResponseEntity<Void> kakaoLogin() {

        String state = UUID.randomUUID().toString();

        ResponseCookie stateCookie =
                ResponseCookie
                        .from("kakaoOAuthState", state)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/api/auth/kakao")
                        .maxAge(Duration.ofMinutes(5))
                        .build();

        String authorizationUrl =
                kakaoOAuthService.getAuthorizationUrl(state);

        return ResponseEntity
                .status(302)
                .header(
                        HttpHeaders.SET_COOKIE,
                        stateCookie.toString()
                )
                .header(
                        HttpHeaders.LOCATION,
                        authorizationUrl
                )
                .build();
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(
                    name = "kakaoOAuthState",
                    required = false
            ) String savedState
    ) {

        // OAuth state 검증
        if (savedState == null || !savedState.equals(state)) {
            throw new IllegalArgumentException(
                    "잘못된 카카오 로그인 요청입니다."
            );
        }

        // 사용한 state 쿠키 삭제
        ResponseCookie stateCookie =
                ResponseCookie
                        .from("kakaoOAuthState", "")
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/api/auth/kakao")
                        .maxAge(0)
                        .build();

        String kakaoAccessToken =
                kakaoOAuthService.getAccessToken(code);

        KakaoUserResponse kakaoUser =
                kakaoOAuthService.getUser(kakaoAccessToken);

        KakaoUserResponse.KakaoAccount account =
                kakaoUser.kakaoAccount();

        if (account == null
                || account.email() == null
                || !Boolean.TRUE.equals(account.emailValid())
                || !Boolean.TRUE.equals(account.emailVerified())) {

            throw new IllegalArgumentException(
                    "카카오 이메일 정보를 확인할 수 없습니다."
            );
        }

        String nickname =
                account.profile() != null
                        && account.profile().nickname() != null
                        ? account.profile().nickname()
                        : "Mood&Move 사용자";

        User user =
                authService.findOrCreateKakaoUser(
                        kakaoUser.id(),
                        account.email(),
                        nickname
                );

        // 탈퇴 신청 중인 계정
        if (authService.isWithdrawalPending(user.getId())) {

            String recoveryToken =
                    jwtProvider.createRecoveryToken(user);

            ResponseCookie recoveryCookie =
                    ResponseCookie
                            .from("recoveryToken", recoveryToken)
                            .httpOnly(true)
                            .secure(cookieSecure)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(
                                    Duration.ofMillis(
                                            jwtProvider.getRecoveryTokenExpirationMillis()
                                    )
                            )
                            .build();

            ResponseCookie accessCookie =
                    ResponseCookie
                            .from("accessToken", "")
                            .httpOnly(true)
                            .secure(cookieSecure)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(0)
                            .build();

            ResponseCookie refreshCookie =
                    ResponseCookie
                            .from("refreshToken", "")
                            .httpOnly(true)
                            .secure(cookieSecure)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(0)
                            .build();

            return ResponseEntity
                    .status(302)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            stateCookie.toString()
                    )
                    .header(
                            HttpHeaders.SET_COOKIE,
                            recoveryCookie.toString()
                    )
                    .header(
                            HttpHeaders.SET_COOKIE,
                            accessCookie.toString()
                    )
                    .header(
                            HttpHeaders.SET_COOKIE,
                            refreshCookie.toString()
                    )
                    .header(
                            HttpHeaders.LOCATION,
                            frontendUrl + "/account-recovery"
                    )
                    .build();
        }

        user = authService.startLoginSession(user.getId());

        String accessToken =
                jwtProvider.createAccessToken(user);

        String refreshToken =
                jwtProvider.createRefreshToken(user);

        LocalDateTime refreshExpiresAt =
                LocalDateTime.ofInstant(
                        jwtProvider.getExpiration(refreshToken)
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
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie
                        .from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(
                                Duration.ofMillis(
                                        jwtProvider.getRefreshTokenExpirationMillis()
                                )
                        )
                        .build();

        return ResponseEntity
                .status(302)
                .header(
                        HttpHeaders.SET_COOKIE,
                        stateCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .header(
                        HttpHeaders.LOCATION,
                        frontendUrl + "/"
                )
                .build();
    }

    @PostMapping("/social/recover/cancel")
    public ResponseEntity<Void> cancelSocialRecovery() {

        ResponseCookie recoveryCookie =
                ResponseCookie
                        .from("recoveryToken", "")
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        recoveryCookie.toString()
                )
                .build();
    }
}
