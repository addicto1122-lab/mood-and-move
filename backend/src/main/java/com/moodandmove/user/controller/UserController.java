package com.moodandmove.user.controller;

import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.dto.request.*;
import com.moodandmove.user.dto.response.HobbyResponse;
import com.moodandmove.user.dto.response.PreferenceResponse;
import com.moodandmove.user.dto.response.WithdrawalStatusResponse;
import com.moodandmove.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        try {
            userService.changePassword(
                    user.getId(),
                    request.currentPassword(),
                    request.newPassword()
            );


            ResponseCookie accessCookie = ResponseCookie
                    .from("accessToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build();

            ResponseCookie refreshCookie = ResponseCookie
                    .from("refreshToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 필수 온보딩
    @PostMapping("/me/onboarding")
    public ResponseEntity<Void> completeOnboarding(
            Authentication authentication,
            @Valid @RequestBody RequiredOnboardingRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        try {
            userService.completeOnboarding(
                    user.getId(),
                    request.ageGroup(),
                    request.gender(),
                    request.hobbyIds()
            );

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 취미 및 선택 선호 수정
    @PatchMapping("/me/preferences")
    public ResponseEntity<Void> updatePreference(
            Authentication authentication,
            @Valid @RequestBody UpdatePreferenceRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        try {
            userService.updatePreference(
                    user.getId(),
                    request.hobbyIds(),
                    request.activityStyle(),
                    request.activityEnvironment(),
                    request.socialPreference(),
                    request.defaultAvailableMinutes(),
                    request.defaultRegionName(),
                    request.defaultRegionCode(),
                    request.defaultRegionLatitude(),
                    request.defaultRegionLongitude()
            );

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 프로필 기본 정보 수정
    @PatchMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        userService.updateProfile(
                user.getId(),
                request.nickname(),
                request.ageGroup(),
                request.gender()
        );

        return ResponseEntity.ok().build();
    }

    // 현재 취미 및 선호 조회
    @GetMapping("/me/preferences")
    public ResponseEntity<PreferenceResponse> getPreferences(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        PreferenceResponse response =
                userService.getPreferences(
                        user.getId()
                );

        return ResponseEntity.ok(response);
    }

    // 전체 취미 목록 조회
    @GetMapping("/hobbies")
    public ResponseEntity<List<HobbyResponse>> getHobbies() {
        return ResponseEntity.ok(
                userService.getHobbies()
        );
    }

    // 회원탈퇴 신청
    @PostMapping("/me/withdrawal")
    public ResponseEntity<Void> requestWithdrawal(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        User user = (User) authentication.getPrincipal();

        try {
            userService.requestWithdrawal(
                    user.getId(),
                    request.currentPassword()
            );

            ResponseCookie accessCookie = ResponseCookie
                    .from("accessToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build();

            ResponseCookie refreshCookie = ResponseCookie
                    .from("refreshToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 회원탈퇴 신청 상태 조회
    @GetMapping("/me/withdrawal")
    public ResponseEntity<WithdrawalStatusResponse> getWithdrawalStatus(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        WithdrawalStatusResponse response =
                userService.getWithdrawalStatus(
                        user.getId()
                );

        return ResponseEntity.ok(response);
    }

    // 회원탈퇴 신청 취소
    @DeleteMapping("/me/withdrawal")
    public ResponseEntity<Void> cancelWithdrawal(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        try {
            userService.cancelWithdrawal(
                    user.getId()
            );

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}