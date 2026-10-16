package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.AccountDeletionOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AccountDeletionOutboxRepository
        extends JpaRepository<AccountDeletionOutbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountDeletionOutbox>
    findFirstByNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
            LocalDateTime now
    );


}