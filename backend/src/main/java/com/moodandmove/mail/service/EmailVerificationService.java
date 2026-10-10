package com.moodandmove.mail.service;

import com.moodandmove.mail.entity.EmailVerification;
import com.moodandmove.mail.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.transaction.annotation.Propagation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    public record VerifyResult(
            boolean success,
            String message,
            String signupToken
    ) {
    }

    private final EmailVerificationRepository repository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

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

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void sendCode(String email) {

        // 1. 이메일 정규화
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        LocalDateTime now = LocalDateTime.now();

        // 2. 기존 인증 정보 조회
        EmailVerification verification = repository
                .findByEmail(normalizedEmail)
                .orElse(null);

        // 3. 1분 이내 재발송 제한
        if (verification != null && now.isBefore(verification.getLastSentAt().plusMinutes(1))) {
            throw new IllegalStateException("인증번호는 1분 후 재발송할 수 있습니다.");
        }

        // 4. 6자리 인증번호 생성
        String code = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        // 5. 인증번호 해시 처리
        String codeHash = passwordEncoder.encode(code);

        // 6. 5분 만료시간
        LocalDateTime expiresAt = now.plusMinutes(5);

        // 7. DB 저장 또는 갱신
        if (verification == null) {
            verification = new EmailVerification(
                    normalizedEmail,
                    codeHash,
                    expiresAt,
                    now
            );

        } else {
            verification.reissue(
                    codeHash,
                    expiresAt,
                    now
            );
        }

        repository.saveAndFlush(verification);

        // 8. 이메일 발송
        mailService.sendVerificationCode(
                normalizedEmail,
                code
        );
    }

    @Transactional
    public VerifyResult verifyCode(String email, String code) {

        // 1. 입력값 확인
        if (email == null || email.isBlank()
                || code == null || !code.matches("\\d{6}")) {
            return new VerifyResult(
                    false, "이메일 또는 인증번호 형식이 올바르지 않습니다.", null);
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        LocalDateTime now = LocalDateTime.now();

        // 2. 인증 정보 조회 및 잠금
        EmailVerification verification = repository
                .findByEmailForUpdate(normalizedEmail)
                .orElse(null);

        if (verification == null) {
            return new VerifyResult(
                    false, "인증번호 발송 내역이 없습니다.", null);
        }

        // 3. 이미 사용하거나 인증한 경우
        if (verification.getUsedAt() != null
                || verification.getVerifiedAt() != null) {
            return new VerifyResult(
                    false, "이미 처리된 인증 요청입니다.", null);
        }

        // 4. 인증번호 만료 확인
        if (!now.isBefore(verification.getExpiresAt())) {
            return new VerifyResult(
                    false, "인증번호가 만료되었습니다.", null);
        }

        // 5. 시도 횟수 확인
        if (verification.getAttemptCount() >= 5) {
            return new VerifyResult(
                    false, "인증 시도 횟수를 초과했습니다.", null);
        }

        // 6. BCrypt 해시 비교
        if (!passwordEncoder.matches(
                code, verification.getCodeHash())) {

            verification.increaseAttemptCount();

            return new VerifyResult(
                    false, "인증번호가 일치하지 않습니다.", null);
        }

        // 7. 가입용 일회성 토큰 생성
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String signupToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        // 8. 토큰 해시 저장 및 인증 성공 처리
        verification.markVerified(
                sha256(signupToken),
                now.plusMinutes(10),
                now
        );

        return new VerifyResult(
                true,
                "이메일 인증이 완료되었습니다.",
                signupToken
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeSignupToken(
            String email,
            String signupToken
    ) {

        // 1. 토큰 존재 여부 확인
        if (signupToken == null || signupToken.isBlank()) {
            throw new IllegalArgumentException(
                    "이메일 인증이 필요합니다."
            );
        }

        // 2. 이메일 정규화
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        LocalDateTime now = LocalDateTime.now();

        // 3. 이메일 인증 정보 조회 및 행 잠금
        EmailVerification verification = repository
                .findByEmailForUpdate(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "이메일 인증 정보가 없습니다."
                        )
                );

        // 4. 인증 완료 여부
        if (verification.getVerifiedAt() == null) {
            throw new IllegalArgumentException(
                    "이메일 인증이 완료되지 않았습니다."
            );
        }

        // 5. 이미 사용한 토큰인지 확인
        if (verification.getUsedAt() != null) {
            throw new IllegalArgumentException(
                    "이미 사용된 이메일 인증 정보입니다."
            );
        }

        // 6. 가입용 토큰 만료 여부 확인
        if (verification.getSignupTokenExpiresAt() == null
                || !now.isBefore(
                verification.getSignupTokenExpiresAt()
        )) {
            throw new IllegalArgumentException(
                    "이메일 인증 유효시간이 만료되었습니다."
            );
        }

        // 7. 가입용 토큰 해시 비교
        String savedHash = verification.getSignupTokenHash();

        if (savedHash == null) {
            throw new IllegalArgumentException(
                    "유효하지 않은 이메일 인증 정보입니다."
            );
        }

        String inputHash = sha256(signupToken);

        if (!MessageDigest.isEqual(
                inputHash.getBytes(StandardCharsets.US_ASCII),
                savedHash.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new IllegalArgumentException(
                    "이메일 인증 토큰이 올바르지 않습니다."
            );
        }

        // 8. 사용 처리
        verification.markSignupUsed(now);
    }

}