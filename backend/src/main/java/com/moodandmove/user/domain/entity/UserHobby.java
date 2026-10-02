package com.moodandmove.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_hobbies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHobby {

    @EmbeddedId
    private UserHobbyId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("hobbyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private UserHobby(
            User user,
            Hobby hobby
    ) {
        this.id = new UserHobbyId(
                user.getId(),
                hobby.getId()
        );

        this.user = user;
        this.hobby = hobby;
    }

    public static UserHobby create(
            User user,
            Hobby hobby
    ) {
        return new UserHobby(
                user,
                hobby
        );
    }
}