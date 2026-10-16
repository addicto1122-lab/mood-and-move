package com.moodandmove.user.service;

import com.moodandmove.recommendation.repository.RecommendationGenerationLogRepository;
import com.moodandmove.user.domain.entity.AccountDeletionOutbox;
import com.moodandmove.user.repository.AccountDeletionOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionOutboxProcessor {

    private final AccountDeletionOutboxRepository
            accountDeletionOutboxRepository;
    private final RecommendationGenerationLogRepository
            generationLogRepository;

    @Transactional
    public boolean processNext(LocalDateTime now) {

        AccountDeletionOutbox outbox =
                accountDeletionOutboxRepository
                        .findFirstByNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                                now
                        )
                        .orElse(null);

        if (outbox == null) {
            return false;
        }

        try {
            generationLogRepository.deleteByUserIdIn(
                    List.of(outbox.getUserId())
            );
        } catch (RuntimeException exception) {
            outbox.recordFailure(exception.getMessage());

            log.warn(
                    "MongoDB account data deletion failed. "
                            + "userId={}, attemptCount={}",
                    outbox.getUserId(),
                    outbox.getAttemptCount(),
                    exception
            );

            return true;
        }

        accountDeletionOutboxRepository.delete(outbox);

        return true;
    }
}