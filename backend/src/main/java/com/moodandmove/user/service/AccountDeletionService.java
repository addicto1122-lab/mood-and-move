package com.moodandmove.user.service;

import com.moodandmove.mail.repository.EmailVerificationRepository;
import com.moodandmove.mail.repository.PasswordResetVerificationRepository;
import com.moodandmove.user.domain.entity.AccountDeletionOutbox;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.domain.entity.UserWithdrawalRequest;
import com.moodandmove.user.repository.AccountDeletionOutboxRepository;
import com.moodandmove.user.repository.UserRepository;
import com.moodandmove.user.repository.UserWithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserWithdrawalRequestRepository withdrawalRequestRepository;
    private final AccountDeletionOutboxRepository accountDeletionOutboxRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetVerificationRepository passwordResetVerificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean deleteIfExpired(Long userId, LocalDateTime now) {

        UserWithdrawalRequest withdrawalRequest =
                withdrawalRequestRepository
                        .findForUpdate(userId)
                        .orElse(null);

        if (withdrawalRequest == null) {return false;}

        if (withdrawalRequest.getDeletionScheduledAt().isAfter(now)) {
            return false;
        }

        User user = withdrawalRequest.getUser();
        String email = user.getEmail();

        accountDeletionOutboxRepository.save(AccountDeletionOutbox.create(userId));

        emailVerificationRepository.deleteByEmail(email);
        passwordResetVerificationRepository.deleteByEmail(email);

        userRepository.delete(user);

        return true;
    }
}