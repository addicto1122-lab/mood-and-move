package com.moodandmove.mood.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mood_entry_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoodEntryActivity {

    @EmbeddedId
    private MoodEntryActivityId id;

    @MapsId("moodEntryId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mood_entry_id")
    private MoodEntry moodEntry;

    @MapsId("activityTagId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_tag_id")
    private ActivityTag activityTag;
}