package com.moodandmove.analysis.controller;

import com.moodandmove.analysis.dto.ActionPersonalStatResponse;
import com.moodandmove.analysis.service.UserActionStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/test/stats")
@RequiredArgsConstructor
public class UserActionStatTestController {

    private final UserActionStatService userActionStatService;

    @PostMapping("/recommendation")
    public String recommendation(
            @RequestParam Long userId,
            @RequestParam Long actionId,
            @RequestParam String emotionCode,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate entryDate
    ) {
        userActionStatService.recordRecommendation(
                userId,
                actionId,
                emotionCode,
                entryDate
        );

        return "recommendation recorded";
    }

    @PostMapping("/execution")
    public String execution(
            @RequestParam Long userId,
            @RequestParam Long actionId,
            @RequestParam String emotionCode,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate entryDate
    ) {
        userActionStatService.recordExecution(
                userId,
                actionId,
                emotionCode,
                entryDate
        );

        return "execution recorded";
    }

    @PostMapping("/recheck")
    public String recheck(
            @RequestParam Long userId,
            @RequestParam Long actionId,
            @RequestParam String emotionCode,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate entryDate,
            @RequestParam int delta
    ) {
        userActionStatService.recordRecheck(
                userId,
                actionId,
                emotionCode,
                entryDate,
                delta
        );

        return "recheck recorded";
    }

    @GetMapping("/personal")
    public ActionPersonalStatResponse getPersonalStat(
            @RequestParam Long userId,
            @RequestParam Long actionId,
            @RequestParam String emotionCode
    ) {

        return userActionStatService.getPersonalStat(
                userId,
                actionId,
                emotionCode
        );
    }
}