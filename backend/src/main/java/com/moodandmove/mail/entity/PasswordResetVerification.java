
package com.moodandmove.mail.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reset_token_hash", length = 64, unique = true)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 인증번호 최초 발급
    public static PasswordResetVerification create(
            String email,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        PasswordResetVerification verification =
                new PasswordResetVerification();

        LocalDateTime now = LocalDateTime.now();

        verification.email = email;
        verification.codeHash = codeHash;
        verification.expiresAt = expiresAt;
        verification.attemptCount = 0;
        verification.lastSentAt = now;
        verification.createdAt = now;
        verification.updatedAt = now;

        return verification;
    }

    // 인증번호 재발송 시 기존 상태 초기화
    public void reissue(
            String codeHash,
            LocalDateTime expiresAt,
            LocalDateTime now
    ) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
        this.lastSentAt = now;

        this.verifiedAt = null;
        this.resetTokenHash = null;
        this.resetTokenExpiresAt = null;
        this.usedAt = null;
        this.updatedAt = now;
    }

    // 인증번호 오류 횟수 증가
    public void increaseAttemptCount() {
        this.attemptCount++;
        this.updatedAt = LocalDateTime.now();
    }

    // 이메일 인증 성공
    public void markVerified(
            String tokenHash,
            LocalDateTime tokenExpiresAt,
            LocalDateTime now
    ) {
        this.verifiedAt = now;
        this.resetTokenHash = tokenHash;
        this.resetTokenExpiresAt = tokenExpiresAt;
        this.updatedAt = now;
    }

    // 비밀번호 재설정 완료
    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
        this.resetTokenHash = null;
        this.resetTokenExpiresAt = null;
        this.updatedAt = now;
    }
}
