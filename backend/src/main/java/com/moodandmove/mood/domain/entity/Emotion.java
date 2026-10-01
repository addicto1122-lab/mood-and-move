package com.moodandmove.mood.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "emotions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Emotion {

    @Id
    @Column(name = "emotion_code", length = 30)
    private String emotionCode;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 10)
    private String emoji;

    @Column(name = "base_score", nullable = false)
    private Integer baseScore;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}