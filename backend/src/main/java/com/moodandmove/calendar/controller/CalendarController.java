package com.moodandmove.calendar.controller;

import com.moodandmove.calendar.dto.CalendarDetailResponse;
import com.moodandmove.calendar.dto.MonthlyCalendarResponse;
import com.moodandmove.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public MonthlyCalendarResponse getMonthlyCalendar(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month
    ){
        return calendarService.getMonthlyCalendar(
                userId,
                year,
                month
        );
    }

    @GetMapping("/{moodEntryId}")
    public CalendarDetailResponse getCalendarDetail(
            @PathVariable Long moodEntryId,
            @RequestParam Long userId
    )
    {
        return calendarService.getCalendarDetail(
                userId,
                moodEntryId
        );
    }
}
