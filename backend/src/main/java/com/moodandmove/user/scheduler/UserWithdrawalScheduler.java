package com.moodandmove.user.scheduler;

import com.moodandmove.user.domain.entity.UserWithdrawalRequest;
import com.moodandmove.user.repository.UserRepository;
import com.moodandmove.user.repository.UserWithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserWithdrawalScheduler {

    private final UserWithdrawalRequestRepository userWithdrawalRequestRepository;
    private final UserRepository userRepository;

    @Scheduled(
            cron = "0 0 3 * * *",
            zone = "Asia/Seoul"
    )

    @Transactional
    public void deleteExpiredAccounts() {

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneId.of("Asia/Seoul")
                );

        List<UserWithdrawalRequest> expiredRequests =
                userWithdrawalRequestRepository
                        .findAllByDeletionScheduledAtLessThanEqual(now);

        if (expiredRequests.isEmpty()) {
            return;
        }

        List<Long> userIds =
                expiredRequests.stream()
                        .map(UserWithdrawalRequest::getUserId)
                        .toList();

        userRepository.deleteAllByIdInBatch(userIds);
    }
}