package com.moodandmove.report.scheduler;

import com.moodandmove.report.service.MonthlyReportDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    private final MonthlyReportDispatchService dispatchService;

    @Scheduled(
            cron = "0 0 9 1 * *",
            zone = "Asia/Seoul"
    )
    public void sendPreviousMonthReport() {

        YearMonth previousMonth =
                YearMonth.now(
                        ZoneId.of("Asia/Seoul")
                ).minusMonths(1);

        dispatchService.sendMonthlyReports(
                previousMonth
        );
    }
}