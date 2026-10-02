package com.moodandmove.calendar.controller;

import com.moodandmove.calendar.dto.CalendarDetailResponse;
import com.moodandmove.calendar.dto.MonthlyCalendarResponse;
import com.moodandmove.calendar.service.CalendarService;
import com.moodandmove.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public MonthlyCalendarResponse getMonthlyCalendar(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month
    ){
        User user = (User) authentication.getPrincipal();

        return calendarService.getMonthlyCalendar(
                user.getId(),
                year,
                month
        );
    }

    @GetMapping("/{moodEntryId}")
    public CalendarDetailResponse getCalendarDetail(
            Authentication authentication,
            @PathVariable Long moodEntryId
    )
    {
        User user = (User) authentication.getPrincipal();

        return calendarService.getCalendarDetail(
                user.getId(),
                moodEntryId
        );
    }
}
