package com.moodandmove.calendar.service;


import com.moodandmove.calendar.dto.CalendarDayResponse;
import com.moodandmove.calendar.dto.CalendarDetailResponse;
import com.moodandmove.calendar.dto.MonthlyCalendarResponse;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.mood.domain.entity.MoodEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final MoodEntryRepository moodEntryRepository;

    public MonthlyCalendarResponse getMonthlyCalendar(
            Long userId,
            int year,
            int month
    ){
        // 2026-10-01
        LocalDate startDate = LocalDate.of(year, month, 1);

        // 2026-11-01
        LocalDate endDate = startDate.plusMonths(1);

        List<MoodEntry> moodEntries =
                moodEntryRepository
                        .findAllByUser_IdAndEntryDateGreaterThanEqualAndEntryDateLessThanAndDeletedAtIsNullOrderByEntryDateAsc
                                (
                                        userId,
                                        startDate,
                                        endDate
                                );

        List<CalendarDayResponse> days = moodEntries.stream()
                .map(this::toCalendarDayResponse)
                .toList();

        return new MonthlyCalendarResponse(
                year,
                month,
                days
        );
    }

    private CalendarDayResponse toCalendarDayResponse(
            MoodEntry moodEntry
    )
    {
        return new CalendarDayResponse(
                moodEntry.getId(),
                moodEntry.getEntryDate(),

                moodEntry.getEmotion().getEmotionCode(),
                moodEntry.getEmotion().getName(),
                moodEntry.getEmotion().getEmoji(),

                moodEntry.getMoodScore(),
                moodEntry.getIntensity()
        );
    }

    public CalendarDetailResponse getCalendarDetail(
            Long userId,
            Long moodEntryId
    ){
        MoodEntry moodEntry = moodEntryRepository
                .findByIdAndUser_IdAndDeletedAtIsNull
                        (
                                moodEntryId,
                                userId
                        )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "해당 일기를 찾을 수 없습니다."
                        )
                );

        return new CalendarDetailResponse(
                moodEntry.getId(),
                moodEntry.getEntryDate(),

                moodEntry.getEmotion().getEmotionCode(),
                moodEntry.getEmotion().getName(),
                moodEntry.getEmotion().getEmoji(),

                moodEntry.getMoodScore(),
                moodEntry.getIntensity(),

                moodEntry.getDiaryContent(),

                moodEntry.getRecommendationStatus().name(),

                moodEntry.getRecordedAt()
        );
    }

}
