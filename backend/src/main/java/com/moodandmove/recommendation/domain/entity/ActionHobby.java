package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.user.domain.entity.Hobby;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "action_hobbies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionHobby {

    @EmbeddedId
    private ActionHobbyId id;

    @MapsId("actionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id")
    private Action action;

    @MapsId("hobbyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;
}