
package com.moodandmove.mail.repository;

import com.moodandmove.mail.entity.PasswordResetVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetVerificationRepository
        extends JpaRepository<PasswordResetVerification, Long> {

    // 이메일로 인증 정보 조회
    Optional<PasswordResetVerification> findByEmail(String email);

    // 인증번호 검증 시 동시 요청 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PasswordResetVerification p
            WHERE p.email = :email
            """)
    Optional<PasswordResetVerification> findByEmailForUpdate(
            @Param("email") String email
    );

    // 재설정 토큰 검증 및 사용 시 동시 요청 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PasswordResetVerification p
            WHERE p.resetTokenHash = :tokenHash
            """)
    Optional<PasswordResetVerification> findByResetTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
