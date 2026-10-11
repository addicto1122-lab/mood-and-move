package com.moodandmove.report.controller;

import com.moodandmove.mail.service.MailService;
import com.moodandmove.report.dto.MonthlyReport;
import com.moodandmove.report.service.MonthlyReportService;
import com.moodandmove.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Profile("local")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;
    private final MailService mailService;

    @PostMapping("/monthly/test")
    public ResponseEntity<Void> sendTestMonthlyReport(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month
    ) {
        User user = (User) authentication.getPrincipal();

        MonthlyReport report =
                monthlyReportService.create(
                        user.getId(),
                        year,
                        month
                );

        mailService.sendMonthlyReport(
                user.getEmail(),
                user.getNickname(),
                report
        );

        return ResponseEntity.ok().build();
    }
}