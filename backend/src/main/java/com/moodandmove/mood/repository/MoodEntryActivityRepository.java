package com.moodandmove.mood.repository;


import com.moodandmove.mood.domain.entity.MoodEntryActivity;
import com.moodandmove.mood.domain.entity.MoodEntryActivityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoodEntryActivityRepository
        extends JpaRepository<MoodEntryActivity, MoodEntryActivityId> {

    List<MoodEntryActivity> findAllByMoodEntry_Id(Long moodEntryId);
}
