package com.moodandmove.user.service;

import com.moodandmove.recommendation.repository.RecommendationGenerationLogRepository;
import com.moodandmove.user.domain.entity.AccountDeletionOutbox;
import com.moodandmove.user.repository.AccountDeletionOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AccountDeletionOutboxProcessorTest {

    private AccountDeletionOutboxRepository
            accountDeletionOutboxRepository;
    private RecommendationGenerationLogRepository
            generationLogRepository;
    private AccountDeletionOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        accountDeletionOutboxRepository =
                mock(AccountDeletionOutboxRepository.class);
        generationLogRepository =
                mock(RecommendationGenerationLogRepository.class);

        processor = new AccountDeletionOutboxProcessor(
                accountDeletionOutboxRepository,
                generationLogRepository
        );
    }

    @Test
    void mongoDeletionSuccess_shouldDeleteOutbox() {

        LocalDateTime now = LocalDateTime.now();
        AccountDeletionOutbox outbox =
                mock(AccountDeletionOutbox.class);

        when(outbox.getUserId())
                .thenReturn(1L);

        when(accountDeletionOutboxRepository
                .findFirstByNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                        now
                ))
                .thenReturn(Optional.of(outbox));

        when(generationLogRepository
                .deleteByUserIdIn(List.of(1L)))
                .thenReturn(3L);

        boolean processed =
                processor.processNext(now);

        assertTrue(processed);

        verify(generationLogRepository)
                .deleteByUserIdIn(List.of(1L));

        verify(accountDeletionOutboxRepository)
                .delete(outbox);
    }

    @Test
    void mongoDeletionFailure_shouldRescheduleOutbox() {

        LocalDateTime now = LocalDateTime.now();
        AccountDeletionOutbox outbox =
                mock(AccountDeletionOutbox.class);

        when(outbox.getUserId())
                .thenReturn(1L);

        when(accountDeletionOutboxRepository
                .findFirstByNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                        now
                ))
                .thenReturn(Optional.of(outbox));

        when(generationLogRepository
                .deleteByUserIdIn(List.of(1L)))
                .thenThrow(
                        new RuntimeException("MongoDB unavailable")
                );

        boolean processed =
                processor.processNext(now);

        assertTrue(processed);

        verify(outbox)
                .recordFailure("MongoDB unavailable");

        verify(accountDeletionOutboxRepository, never())
                .delete(any(AccountDeletionOutbox.class));
    }
}