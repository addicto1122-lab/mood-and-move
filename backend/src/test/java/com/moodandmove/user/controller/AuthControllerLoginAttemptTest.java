
package com.moodandmove.user.controller;

import com.moodandmove.common.exception.InvalidCredentialsException;
import com.moodandmove.common.security.JwtProvider;
import com.moodandmove.user.dto.request.LoginRequest;
import com.moodandmove.user.service.AuthService;
import com.moodandmove.user.service.KakaoOAuthService;
import com.moodandmove.user.service.LoginAttemptLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthControllerLoginAttemptTest {

    private AuthService authService;
    private LoginAttemptLimiter limiter;
    private AuthController controller;
    private HttpServletRequest httpRequest;

    private final String ip = "127.0.0.1";
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        limiter = mock(LoginAttemptLimiter.class);
        httpRequest = mock(HttpServletRequest.class);

        controller = new AuthController(
                authService,
                mock(JwtProvider.class),
                mock(KakaoOAuthService.class),
                limiter
        );

        when(httpRequest.getRemoteAddr()).thenReturn(ip);
    }

    @Test
    void invalidCredentials_shouldRecordFailure() {
        LoginRequest request = new LoginRequest(email, "wrong-password");

        when(authService.recoverAccount(request))
                .thenThrow(new InvalidCredentialsException());

        assertThrows(
                InvalidCredentialsException.class,
                () -> controller.recoverAccount(request, httpRequest)
        );

        verify(limiter).check(ip, email);
        verify(limiter).recordFailure(ip, email);
        verify(limiter, never()).clear(ip, email);
    }

    @Test
    void notWithdrawalPending_shouldNotRecordFailure() {
        LoginRequest request = new LoginRequest(email, "correct-password");

        when(authService.recoverAccount(request))
                .thenThrow(
                        new IllegalArgumentException("탈퇴 신청 상태가 아닙니다.")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.recoverAccount(request, httpRequest)
        );

        verify(limiter).check(ip, email);
        verify(limiter, never()).recordFailure(anyString(), anyString());
        verify(limiter, never()).clear(ip, email);
    }
}
