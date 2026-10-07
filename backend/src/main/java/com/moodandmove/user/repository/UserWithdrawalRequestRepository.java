package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.UserWithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserWithdrawalRequestRepository
        extends JpaRepository<UserWithdrawalRequest, Long> {

    Optional<UserWithdrawalRequest> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);

    List<UserWithdrawalRequest>
    findAllByDeletionScheduledAtLessThanEqual(LocalDateTime now);
}