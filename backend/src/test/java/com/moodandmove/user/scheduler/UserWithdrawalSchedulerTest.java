package com.moodandmove.user.scheduler;

import com.moodandmove.user.domain.entity.UserWithdrawalRequest;
import com.moodandmove.user.repository.UserWithdrawalRequestRepository;
import com.moodandmove.user.service.AccountDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserWithdrawalSchedulerTest {

    private UserWithdrawalRequestRepository
            withdrawalRequestRepository;
    private AccountDeletionService
            accountDeletionService;
    private UserWithdrawalScheduler scheduler;

    @BeforeEach
    void setUp() {
        withdrawalRequestRepository =
                mock(UserWithdrawalRequestRepository.class);
        accountDeletionService =
                mock(AccountDeletionService.class);

        scheduler = new UserWithdrawalScheduler(
                withdrawalRequestRepository,
                accountDeletionService
        );
    }

    @Test
    void oneAccountFailure_shouldNotStopOtherAccounts() {

        UserWithdrawalRequest firstRequest =
                mock(UserWithdrawalRequest.class);
        UserWithdrawalRequest secondRequest =
                mock(UserWithdrawalRequest.class);

        when(firstRequest.getUserId())
                .thenReturn(1L);
        when(secondRequest.getUserId())
                .thenReturn(2L);

        when(withdrawalRequestRepository
                .findAllByDeletionScheduledAtLessThanEqual(
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(
                        secondRequest,
                        firstRequest
                ));

        doThrow(new RuntimeException("MySQL deletion failed"))
                .when(accountDeletionService)
                .deleteIfExpired(
                        eq(1L),
                        any(LocalDateTime.class)
                );

        scheduler.deleteExpiredAccounts();

        InOrder inOrder =
                inOrder(accountDeletionService);

        inOrder.verify(accountDeletionService)
                .deleteIfExpired(
                        eq(1L),
                        any(LocalDateTime.class)
                );

        inOrder.verify(accountDeletionService)
                .deleteIfExpired(
                        eq(2L),
                        any(LocalDateTime.class)
                );
    }
}