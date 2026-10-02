package com.moodandmove.user.controller;

import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.dto.request.ChangePasswordRequest;
import com.moodandmove.user.dto.request.UpdateNicknameRequest;
import com.moodandmove.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(
            Authentication authentication,
            @Valid @RequestBody UpdateNicknameRequest request
            ){
        User user = (User) authentication.getPrincipal();

        userService.updateNickname(
                user.getId(),
                request.nickname()
        );

        return ResponseEntity.ok().build();
    }

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

            ResponseCookie cookie = ResponseCookie
                    .from("accessToken", "")
                    .httpOnly(true)
//                   배포시 수정
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build();

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.SET_COOKIE,
                            cookie.toString()
                    )
                    .build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
