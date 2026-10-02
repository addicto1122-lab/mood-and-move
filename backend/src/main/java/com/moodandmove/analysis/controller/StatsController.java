package com.moodandmove.analysis.controller;

import com.moodandmove.analysis.dto.MonthlyStatsResponse;
import com.moodandmove.analysis.service.UserActionStatService;
import com.moodandmove.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserActionStatService userActionStatService;

    @GetMapping("/monthly")
    public MonthlyStatsResponse getMonthlyStats(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month
    ){
        User user = (User) authentication.getPrincipal();

        return userActionStatService
                .getMonthlyStats(
                        user.getId(),
                        year,
                        month
                );
    }
}
