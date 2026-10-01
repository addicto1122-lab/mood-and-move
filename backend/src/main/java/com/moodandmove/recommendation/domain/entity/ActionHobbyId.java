package com.moodandmove.recommendation.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ActionHobbyId implements Serializable {

    private Long actionId;
    private Long hobbyId;
}