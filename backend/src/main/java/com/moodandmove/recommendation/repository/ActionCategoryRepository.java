package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.entity.ActionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionCategoryRepository extends JpaRepository<ActionCategory, String> {
}
