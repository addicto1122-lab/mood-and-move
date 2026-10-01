package com.moodandmove.recommendation.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recommendation_id",
            nullable = false,
            unique = true
    )
    private Recommendation recommendation;

    @Column(length = 30)
    private String provider;

    @Column(name = "external_place_id", length = 100)
    private String externalPlaceId;

    @Column(name = "place_name", nullable = false, length = 150)
    private String placeName;

    @Column(name = "place_category", length = 50)
    private String placeCategory;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}