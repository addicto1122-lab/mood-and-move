package com.moodandmove.user.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserHobbyId implements Serializable {

    private Long userId;
    private Long hobbyId;
}