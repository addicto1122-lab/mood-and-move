
package com.moodandmove.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

@Component
public class LoginAttemptLimiter {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS =
            Duration.ofMinutes(10).toMillis();

    private final Cache<String, FailureWindow> failures =
            Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofMinutes(20))
                    .maximumSize(50_000)
                    .build();

    // 로그인 전에 제한 여부 확인
    public void check(String ip, String email) {
        FailureWindow window =
                failures.getIfPresent(key(ip, email));

        if (window != null && window.isBlocked()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    // 비밀번호 검증 실패 기록
    public void recordFailure(String ip, String email) {
        failures.get(
                key(ip, email),
                k -> new FailureWindow()
        ).addFailure();
    }

    // 로그인 성공 시 기록 초기화
    public void clear(String ip, String email) {
        failures.invalidate(key(ip, email));
    }

    private String key(String ip, String email) {
        return ip + "|" +
                email.trim().toLowerCase(Locale.ROOT);
    }

    private static class FailureWindow {
        private final Deque<Long> timestamps =
                new ArrayDeque<>();

        synchronized boolean isBlocked() {
            removeExpired();
            return timestamps.size() >= MAX_FAILURES;
        }

        synchronized void addFailure() {
            removeExpired();
            timestamps.addLast(System.currentTimeMillis());
        }

        private void removeExpired() {
            long cutoff =
                    System.currentTimeMillis() - WINDOW_MS;

            while (!timestamps.isEmpty()
                    && timestamps.peekFirst() <= cutoff) {
                timestamps.removeFirst();
            }
        }
    }
}
