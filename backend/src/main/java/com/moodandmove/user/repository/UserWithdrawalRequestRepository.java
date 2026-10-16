package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.UserWithdrawalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserWithdrawalRequestRepository
        extends JpaRepository<UserWithdrawalRequest, Long> {

    Optional<UserWithdrawalRequest> findByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT request
            FROM UserWithdrawalRequest request
            WHERE request.userId = :userId
            """)
    Optional<UserWithdrawalRequest> findForUpdate(
            @Param("userId") Long userId
    );

    boolean existsByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);

    List<UserWithdrawalRequest>
    findAllByDeletionScheduledAtLessThanEqual(LocalDateTime now);
}