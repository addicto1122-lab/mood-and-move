package com.moodandmove.calendar.dto;

import java.util.List;

public record MonthlyCalendarResponse (
        int year,
        int month,
        List<CalendarDayResponse> days
){
}
