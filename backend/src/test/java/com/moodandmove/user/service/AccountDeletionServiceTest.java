package com.moodandmove.user.service;

import com.moodandmove.mail.repository.EmailVerificationRepository;
import com.moodandmove.mail.repository.PasswordResetVerificationRepository;
import com.moodandmove.user.domain.entity.AccountDeletionOutbox;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.domain.entity.UserWithdrawalRequest;
import com.moodandmove.user.repository.AccountDeletionOutboxRepository;
import com.moodandmove.user.repository.UserRepository;
import com.moodandmove.user.repository.UserWithdrawalRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountDeletionServiceTest {

    private UserWithdrawalRequestRepository
            withdrawalRequestRepository;
    private AccountDeletionOutboxRepository
            accountDeletionOutboxRepository;
    private EmailVerificationRepository
            emailVerificationRepository;
    private PasswordResetVerificationRepository
            passwordResetVerificationRepository;
    private UserRepository userRepository;
    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        withdrawalRequestRepository =
                mock(UserWithdrawalRequestRepository.class);
        accountDeletionOutboxRepository =
                mock(AccountDeletionOutboxRepository.class);
        emailVerificationRepository =
                mock(EmailVerificationRepository.class);
        passwordResetVerificationRepository =
                mock(PasswordResetVerificationRepository.class);
        userRepository =
                mock(UserRepository.class);

        service = new AccountDeletionService(
                withdrawalRequestRepository,
                accountDeletionOutboxRepository,
                emailVerificationRepository,
                passwordResetVerificationRepository,
                userRepository
        );
    }

    @Test
    void expiredRequest_shouldCreateOutboxAndDeleteMysqlData() {

        Long userId = 1L;
        String email = "test@example.com";
        LocalDateTime now = LocalDateTime.now();

        UserWithdrawalRequest withdrawalRequest =
                mock(UserWithdrawalRequest.class);
        User user = mock(User.class);

        when(withdrawalRequestRepository.findForUpdate(userId))
                .thenReturn(Optional.of(withdrawalRequest));

        when(withdrawalRequest.getDeletionScheduledAt())
                .thenReturn(now.minusMinutes(1));

        when(withdrawalRequest.getUser())
                .thenReturn(user);

        when(user.getEmail())
                .thenReturn(email);

        boolean deleted =
                service.deleteIfExpired(userId, now);

        assertTrue(deleted);

        ArgumentCaptor<AccountDeletionOutbox> captor =
                ArgumentCaptor.forClass(
                        AccountDeletionOutbox.class
                );

        verify(accountDeletionOutboxRepository)
                .save(captor.capture());

        assertEquals(
                userId,
                captor.getValue().getUserId()
        );

        verify(emailVerificationRepository)
                .deleteByEmail(email);

        verify(passwordResetVerificationRepository)
                .deleteByEmail(email);

        verify(userRepository)
                .delete(user);
    }

    @Test
    void recoveredBeforeLock_shouldSkipDeletion() {

        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        when(withdrawalRequestRepository.findForUpdate(userId))
                .thenReturn(Optional.empty());

        boolean deleted =
                service.deleteIfExpired(userId, now);

        assertFalse(deleted);

        verifyNoInteractions(
                accountDeletionOutboxRepository,
                emailVerificationRepository,
                passwordResetVerificationRepository,
                userRepository
        );
    }
}