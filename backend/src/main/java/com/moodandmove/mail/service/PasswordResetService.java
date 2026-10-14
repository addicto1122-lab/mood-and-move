
package com.moodandmove.mail.service;

import com.moodandmove.mail.entity.PasswordResetVerification;
import com.moodandmove.mail.repository.PasswordResetVerificationRepository;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.repository.UserRepository;
import com.moodandmove.user.repository.RefreshTokenRepository;
import com.moodandmove.user.repository.UserWithdrawalRequestRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetVerificationRepository repository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserWithdrawalRequestRepository withdrawalRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public record VerifyResult(
            boolean success,
            String message,
            String resetToken
    ) {}

    // 1. 비밀번호 재설정 인증번호 발송
    @Transactional
    public void sendCode(String rawEmail) {

        String email = normalizeEmail(rawEmail);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        // 존재하지 않거나 소셜 전용 계정이면 발송하지 않음
        // API 응답에서는 계정 존재 여부를 공개하지 않아야 함
        if (user == null || !user.isLocalLoginEnabled()) {
            return;
        }

        if (withdrawalRepository.existsByUser_Id(user.getId())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        PasswordResetVerification verification =
                repository.findByEmailForUpdate(email)
                        .orElse(null);

        // 1분 이내 중복 발송 방지
        if (verification != null
                && now.isBefore(
                verification.getLastSentAt().plusMinutes(1))) {
            return;
        }

        String code = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiresAt = now.plusMinutes(5);

        if (verification == null) {
            verification = PasswordResetVerification.create(
                    email,
                    codeHash,
                    expiresAt
            );
        } else {
            verification.reissue(codeHash, expiresAt, now);
        }

        repository.saveAndFlush(verification);

        mailService.sendPasswordResetCode(email, code);
    }

    // 2. 인증번호 검증
    @Transactional
    public VerifyResult verifyCode(String rawEmail, String code) {

        if (rawEmail == null || rawEmail.isBlank()
                || code == null || !code.matches("\\d{6}")) {
            return new VerifyResult(
                    false, "인증 정보가 올바르지 않습니다.", null);
        }

        String email = normalizeEmail(rawEmail);
        LocalDateTime now = LocalDateTime.now();

        PasswordResetVerification verification =
                repository.findByEmailForUpdate(email)
                        .orElse(null);

        if (verification == null
                || verification.getUsedAt() != null
                || verification.getVerifiedAt() != null) {
            return new VerifyResult(
                    false, "유효하지 않은 인증 요청입니다.", null);
        }

        if (!now.isBefore(verification.getExpiresAt())) {
            return new VerifyResult(
                    false, "인증번호가 만료되었습니다.", null);
        }

        if (verification.getAttemptCount() >= 5) {
            return new VerifyResult(
                    false, "인증 시도 횟수를 초과했습니다.", null);
        }

        if (!passwordEncoder.matches(
                code, verification.getCodeHash())) {

            verification.increaseAttemptCount();

            return new VerifyResult(
                    false, "인증번호가 일치하지 않습니다.", null);
        }

        // 인증 성공 시 일회용 재설정 토큰 생성
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String resetToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        verification.markVerified(
                sha256(resetToken),
                now.plusMinutes(10),
                now
        );

        return new VerifyResult(
                true,
                "이메일 인증이 완료되었습니다.",
                resetToken
        );
    }

    // 3. 새 비밀번호 설정
    @Transactional
    public void resetPassword(
            String resetToken,
            String newPassword
    ) {

        if (resetToken == null || resetToken.isBlank()) {
            throw new IllegalArgumentException(
                    "비밀번호 재설정 토큰이 필요합니다.");
        }

        if (newPassword == null
                || newPassword.length() < 8
                || newPassword.length() > 50) {
            throw new IllegalArgumentException(
                    "비밀번호는 8자 이상 50자 이하로 입력해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();

        PasswordResetVerification verification =
                repository.findByResetTokenHashForUpdate(
                        sha256(resetToken)
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "유효하지 않은 재설정 토큰입니다.")
                );

        if (verification.getUsedAt() != null
                || verification.getVerifiedAt() == null
                || verification.getResetTokenExpiresAt() == null
                || !now.isBefore(
                verification.getResetTokenExpiresAt())) {

            throw new IllegalArgumentException(
                    "만료되었거나 사용된 재설정 토큰입니다.");
        }

        User existingUser = userRepository
                .findByEmail(verification.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "비밀번호를 재설정할 수 없습니다.")
                );

        // 사용자 잠금: 다른 토큰 갱신 작업과 충돌 방지
        User user = userRepository.findForUpdate(
                existingUser.getId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "비밀번호를 재설정할 수 없습니다.")
        );

        if (!user.isLocalLoginEnabled()
                || withdrawalRepository.existsByUser_Id(user.getId())) {
            throw new IllegalArgumentException(
                    "비밀번호를 재설정할 수 없습니다.");
        }

        if (passwordEncoder.matches(
                newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "기존 비밀번호와 다른 비밀번호를 입력해주세요.");
        }

        // 새로운 비밀번호 저장
        user.updatePassword(
                passwordEncoder.encode(newPassword)
        );

        // 모든 Refresh Token 폐기
        refreshTokenRepository.deleteAllByUser_Id(user.getId());
        refreshTokenRepository.flush();

        // 기존 Access Token 무효화
        user.increaseTokenVersion();

        // 재설정 토큰 재사용 방지
        verification.markUsed(now);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "이메일을 입력해주세요.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
