// backend/src/main/java/com/moodandmove/user/repository/RefreshTokenRepository.java
package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken>
    findByUser_IdAndTokenVersion(
            Long userId,
            int tokenVersion
    );

    void deleteAllByUser_Id(Long userId);
}