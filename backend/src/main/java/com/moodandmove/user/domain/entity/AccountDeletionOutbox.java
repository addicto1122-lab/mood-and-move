package com.moodandmove.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "account_deletion_outbox",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_account_deletion_outbox_user",
                columnNames = "user_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountDeletionOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private Long userId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    private AccountDeletionOutbox(Long userId) {
        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        this.userId = userId;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public static AccountDeletionOutbox create(Long userId) {
        return new AccountDeletionOutbox(userId);
    }

    public void recordFailure(String errorMessage) {

        if (attemptCount < Integer.MAX_VALUE) {
            attemptCount++;
        }

        int exponent =
                Math.min(attemptCount - 1, 6);

        long delayMinutes =
                Math.min(1L << exponent, 60L);

        nextAttemptAt =
                LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                        .plusMinutes(delayMinutes);

        String normalizedError =
                errorMessage == null || errorMessage.isBlank()
                        ? "Unknown MongoDB deletion error"
                        : errorMessage;

        lastError = normalizedError.substring(
                0,
                Math.min(normalizedError.length(), 1000)
        );
    }
}