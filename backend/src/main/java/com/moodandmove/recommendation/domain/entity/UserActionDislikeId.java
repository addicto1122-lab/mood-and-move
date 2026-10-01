package com.moodandmove.recommendation.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserActionDislikeId implements Serializable {

    private Long userId;
    private Long actionId;
}