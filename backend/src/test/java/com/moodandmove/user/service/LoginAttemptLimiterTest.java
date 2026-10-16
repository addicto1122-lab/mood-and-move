
package com.moodandmove.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginAttemptLimiter();
    }

    @Test
    void fiveFailures_shouldBlockNextAttempt() {
        String ip = "127.0.0.1";
        String email = "test@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.check(ip, email);
            limiter.recordFailure(ip, email);
        }

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> limiter.check(ip, email)
                );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );
    }

    @Test
    void differentIp_shouldNotBeBlocked() {
        String email = "test@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("127.0.0.1", email);
        }

        assertDoesNotThrow(
                () -> limiter.check("127.0.0.2", email)
        );
    }

    @Test
    void differentEmail_shouldNotBeBlocked() {
        String ip = "127.0.0.1";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ip, "test@example.com");
        }

        assertDoesNotThrow(
                () -> limiter.check(ip, "other@example.com")
        );
    }

    @Test
    void success_shouldClearFailureRecords() {
        String ip = "127.0.0.1";
        String email = "test@example.com";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(ip, email);
        }

        limiter.clear(ip, email);

        assertDoesNotThrow(
                () -> limiter.check(ip, email)
        );
    }

    @Test
    void email_shouldBeCaseInsensitive() {
        String ip = "127.0.0.1";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(
                    ip,
                    "TEST@EXAMPLE.COM"
            );
        }

        assertThrows(
                ResponseStatusException.class,
                () -> limiter.check(
                        ip,
                        "test@example.com"
                )
        );
    }
}
