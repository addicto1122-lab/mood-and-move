package com.moodandmove.analysis.controller;

import com.moodandmove.analysis.dto.MonthlyStatsResponse;
import com.moodandmove.analysis.service.UserActionStatService;
import lombok.RequiredArgsConstructor;
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
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month
    ){
        return userActionStatService
                .getMonthlyStats(
                        userId,
                        year,
                        month
                );
    }
}
