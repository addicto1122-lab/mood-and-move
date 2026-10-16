package com.moodandmove.user.scheduler;

import com.moodandmove.user.service.AccountDeletionOutboxProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountDeletionOutboxScheduler {

    private static final int MAX_TASKS_PER_RUN = 100;

    private final AccountDeletionOutboxProcessor
            accountDeletionOutboxProcessor;

    @Scheduled(
            fixedDelayString =
                    "${account-deletion.outbox.fixed-delay-ms:60000}"
    )
    public void processPendingTasks() {

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneId.of("Asia/Seoul")
                );

        for (int count = 0;
             count < MAX_TASKS_PER_RUN;
             count++) {

            try {
                boolean processed =
                        accountDeletionOutboxProcessor
                                .processNext(now);

                if (!processed) {
                    return;
                }
            } catch (RuntimeException exception) {
                log.error(
                        "Account deletion outbox processing failed.",
                        exception
                );

                return;
            }
        }
    }
}