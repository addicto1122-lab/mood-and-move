package com.moodandmove.user.scheduler;

import com.moodandmove.user.domain.entity.UserWithdrawalRequest;
import com.moodandmove.user.repository.UserWithdrawalRequestRepository;
import com.moodandmove.user.service.AccountDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawalScheduler {

    private final UserWithdrawalRequestRepository
            withdrawalRequestRepository;
    private final AccountDeletionService
            accountDeletionService;

    @Scheduled(
            cron = "0 0 3 * * *",
            zone = "Asia/Seoul"
    )
    public void deleteExpiredAccounts() {

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneId.of("Asia/Seoul")
                );

        List<Long> userIds =
                withdrawalRequestRepository
                        .findAllByDeletionScheduledAtLessThanEqual(now)
                        .stream()
                        .map(UserWithdrawalRequest::getUserId)
                        .sorted()
                        .toList();

        for (Long userId : userIds) {
            try {
                accountDeletionService.deleteIfExpired(
                        userId,
                        now
                );
            } catch (RuntimeException exception) {
                log.error(
                        "Failed to delete expired account. userId={}",
                        userId,
                        exception
                );
            }
        }
    }
}