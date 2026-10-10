package com.moodandmove.mail.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "email_verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "signup_token_hash",
            columnDefinition = "CHAR(64)")
    private String signupTokenHash;

    @Column(name = "signup_token_expires_at")
    private LocalDateTime signupTokenExpiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "created_at",
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public EmailVerification(
            String email,
            String codeHash,
            LocalDateTime expiresAt,
            LocalDateTime lastSentAt
    ) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = lastSentAt;
        this.attemptCount = 0;
    }

    public void reissue(
            String codeHash,
            LocalDateTime expiresAt,
            LocalDateTime now
    ) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = now;
        this.attemptCount = 0;

        this.verifiedAt = null;
        this.signupTokenHash = null;
        this.signupTokenExpiresAt = null;
        this.usedAt = null;
    }

    // 인증번호 검증 실패 횟수 증가
    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    // 이메일 인증 성공 처리
    public void markVerified(
            String signupTokenHash,
            LocalDateTime signupTokenExpiresAt,
            LocalDateTime now
    ) {
        this.verifiedAt = now;
        this.signupTokenHash = signupTokenHash;
        this.signupTokenExpiresAt = signupTokenExpiresAt;
    }

    public void markSignupUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}