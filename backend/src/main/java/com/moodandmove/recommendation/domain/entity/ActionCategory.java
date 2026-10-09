package com.moodandmove.recommendation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "action_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionCategory {

    // 카테고리 이름
    @Id
    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    // 화면에 표시할 한국어 라벨
    @Column(name = "category_name", nullable = false)
    private String categoryName;

    // 카테고리 대표 이모지
    @Column(name = "emoji", nullable = false)
    private String emoji;
}