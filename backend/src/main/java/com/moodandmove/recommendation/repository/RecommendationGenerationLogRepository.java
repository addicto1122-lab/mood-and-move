package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.document.RecommendationGenerationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RecommendationGenerationLogRepository
        extends MongoRepository<RecommendationGenerationLog, String> {
    long deleteByUserIdIn(List<Long> userIds);
}
