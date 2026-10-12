package com.moodandmove.recommendation.repository;

import com.moodandmove.recommendation.domain.document.RecommendationGenerationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecommendationGenerationLogRepository
        extends MongoRepository<RecommendationGenerationLog, String> {
}
