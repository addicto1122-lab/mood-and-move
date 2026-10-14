package com.moodandmove.user.scheduler;

import com.moodandmove.recommendation.repository.RecommendationGenerationLogRepository;
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
    private final RecommendationGenerationLogRepository generationLogRepository;

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

        // MongoDB 로그 삭제 실패 시 예외를 전파하여 회원 삭제를 중단한다.
        // 탈퇴 요청이 남아 다음 스케줄 실행 때 다시 시도한다.
        generationLogRepository.deleteByUserIdIn(userIds);

        // MongoDB 로그 삭제 후 MySQL 회원 삭제
        userRepository.deleteAllByIdInBatch(userIds);
    }
}