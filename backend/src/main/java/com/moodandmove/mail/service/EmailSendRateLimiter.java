package com.moodandmove.mail.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

@Component
public class EmailSendRateLimiter {

    // IP별 요청 기록
    private final Cache<String, Window> ipRequests =
            Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofMinutes(20))
                    .maximumSize(50_000)
                    .build();

    // 이메일별 요청 기록
    private final Cache<String, Window> emailRequests =
            Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofHours(2))
                    .maximumSize(50_000)
                    .build();

    public void check(String ip, String email) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        // 동일 IP: 10분 최대 10회
        Window ipWindow = ipRequests.get(ip, key -> new Window());

        if (!ipWindow.allow(10, Duration.ofMinutes(10))) {
            throw new IllegalStateException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        // 동일 이메일: 1시간 최대 3회
        Window emailWindow = emailRequests.get(normalizedEmail, key -> new Window());

        if (!emailWindow.allow(3, Duration.ofHours(1))) {
            throw new IllegalStateException("해당 이메일의 인증 요청 횟수를 초과했습니다.");
        }
    }

    private static class Window {

        private final Deque<Long> timestamps = new ArrayDeque<>();

        public synchronized boolean allow(
                int limit,
                Duration duration
        ) {
            long now = System.currentTimeMillis();
            long cutoff = now - duration.toMillis();

            // 제한시간이 지난 요청 기록 제거
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.removeFirst();
            }

            // 허용 횟수 초과
            if (timestamps.size() >= limit) {
                return false;
            }

            // 현재 요청 기록
            timestamps.addLast(now);

            return true;
        }
    }
}