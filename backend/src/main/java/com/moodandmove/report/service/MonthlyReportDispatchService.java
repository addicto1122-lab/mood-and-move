package com.moodandmove.report.service;

import com.moodandmove.mail.service.MailService;
import com.moodandmove.report.dto.MonthlyReport;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyReportDispatchService {

    private final UserRepository userRepository;
    private final MonthlyReportService monthlyReportService;
    private final MailService mailService;

    public void sendMonthlyReports(YearMonth targetMonth) {

        List<User> users = userRepository.findAll();

        for (User user : users) {

            try {
                MonthlyReport report =
                        monthlyReportService.create(
                                user.getId(),
                                targetMonth.getYear(),
                                targetMonth.getMonthValue()
                        );

                /*
                 * 지난달 기록이 전혀 없다면
                 * 리포트 발송하지 않음
                 */
                if (report.diaryCount() == 0) {
                    continue;
                }

                mailService.sendMonthlyReport(
                        user.getEmail(),
                        user.getNickname(),
                        report
                );

                System.out.println(
                        "월간 리포트 발송 성공"
                                + " userId=" + user.getId()
                                + " / month=" + targetMonth
                );

            } catch (Exception e) {

                /*
                 * 한 사용자 실패 때문에
                 * 전체 발송이 중단되지 않도록
                 */
                System.err.println(
                        "월간 리포트 발송 실패"
                                + " userId=" + user.getId()
                                + " / " + e.getMessage()
                );
            }
        }
    }
}