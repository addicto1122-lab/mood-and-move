package com.moodandmove.mood.repository;

import com.moodandmove.mood.domain.entity.MoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoodEntryRepository extends JpaRepository<MoodEntry,Long> {

    List<MoodEntry> findAllByUser_IdAndEntryDateGreaterThanEqualAndEntryDateLessThanAndDeletedAtIsNullOrderByEntryDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<MoodEntry> findByIdAndUser_IdAndDeletedAtIsNull(
            Long moodEntryId,
            Long userId
    );

}
