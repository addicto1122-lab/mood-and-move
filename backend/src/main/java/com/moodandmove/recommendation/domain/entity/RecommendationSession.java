
package com.moodandmove.recommendation.domain.entity;

import com.moodandmove.common.domain.type.TimeBucket;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.recommendation.domain.type.RecommendationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "recommendation_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추천을 요청한 사용자 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 기분 입력 1건당 추천 세션 최대 1개
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "mood_entry_id",
            nullable = false,
            unique = true
    )
    private MoodEntry moodEntry;

    // COLD_START / HYBRID / PERSONALIZED
    @Enumerated(EnumType.STRING)
    @Column(
            name = "recommendation_type",
            nullable = false,
            length = 20
    )
    private RecommendationType recommendationType;

    // MORNING / AFTERNOON / EVENING / NIGHT
    @Enumerated(EnumType.STRING)
    @Column(
            name = "time_bucket",
            nullable = false,
            length = 20
    )
    private TimeBucket timeBucket;

    // 추천 요청 시각
    @CreationTimestamp
    @Column(
            name = "requested_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime requestedAt;

    // 추천 세션 생성
    public static RecommendationSession create(
            Long userId,
            MoodEntry moodEntry,
            TimeBucket timeBucket,
            RecommendationType recommendationType
    ) {
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(moodEntry, "기분 입력은 필수입니다.");
        Objects.requireNonNull(timeBucket, "시간대는 필수입니다.");
        Objects.requireNonNull(recommendationType, "추천 유형은 필수입니다.");

        RecommendationSession session = new RecommendationSession();

        session.userId = userId;
        session.moodEntry = moodEntry;
        session.timeBucket = timeBucket;
        session.recommendationType = recommendationType;

        return session;
    }
}
