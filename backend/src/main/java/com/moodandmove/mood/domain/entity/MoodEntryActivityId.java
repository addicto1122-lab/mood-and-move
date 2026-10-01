package com.moodandmove.mood.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MoodEntryActivityId implements Serializable {

    private Long moodEntryId;
    private Long activityTagId;
}