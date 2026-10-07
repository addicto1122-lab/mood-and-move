package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_action_dislikes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActionDislike {

    @EmbeddedId
    private UserActionDislikeId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("actionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id")
    private Action action;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private UserActionDislike(User user, Action action) {
        this.id = new UserActionDislikeId(user.getId(), action.getId());

        this.user = user;
        this.action = action;
    }

    public static UserActionDislike create(User user, Action action) {
        return new UserActionDislike(user, action);
    }
}