package com.moodandmove.mood.domain.entity;

import com.moodandmove.mood.domain.type.RecommendationStatus;
import com.moodandmove.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "mood_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mood_entries_user_date",
                columnNames = {"user_id", "entry_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emotion_code", nullable = false)
    private Emotion emotion;

    @Column(name = "mood_score", nullable = false)
    private Integer moodScore;

    @Column(nullable = false)
    private Integer intensity;

    @Column(
            name = "current_activity",
            nullable = false,
            length = 100
    )
    private String currentActivity;

    @Column(name = "diary_content", columnDefinition = "TEXT")
    private String diaryContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_status", nullable = false, length = 20)
    private RecommendationStatus recommendationStatus =
            RecommendationStatus.AVAILABLE;

    @Column(name = "recommendation_eligible_until")
    private LocalDateTime recommendationEligibleUntil;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static MoodEntry create(
            User user,
            Emotion emotion,
            Integer intensity,
            Integer moodScore,
            String currentActivity,
            String diaryContent
    ) {
        MoodEntry moodEntry = new MoodEntry();

        LocalDateTime now = LocalDateTime.now();

        moodEntry.user = user;
        moodEntry.entryDate = now.toLocalDate();
        moodEntry.emotion = emotion;
        moodEntry.moodScore = moodScore;
        moodEntry.intensity = intensity;
        moodEntry.currentActivity = currentActivity;
        moodEntry.diaryContent = diaryContent;
        moodEntry.recommendationStatus = RecommendationStatus.AVAILABLE;
        moodEntry.recordedAt = now;

        return moodEntry;
    }
}