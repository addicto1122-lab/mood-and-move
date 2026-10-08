package com.moodandmove.home.service;

import com.moodandmove.home.dto.response.HomeResponse;
import com.moodandmove.mood.domain.entity.MoodEntry;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final MoodEntryRepository moodEntryRepository;
    private final RecommendationRepository recommendationRepository;

    @Transactional(readOnly = true)
    public HomeResponse getHome(Long userId) {

        MoodEntry latestMoodEntry = moodEntryRepository
                        .findFirstByUser_IdAndDeletedAtIsNullOrderByEntryDateDescRecordedAtDesc(userId)
                        .orElse(null);

        if (latestMoodEntry == null) {
            return new HomeResponse(null, null);
        }

        HomeResponse.LatestMood latestMood = HomeResponse.LatestMood.from(latestMoodEntry);

        HomeResponse.TodayRecommendation todayRecommendation = null;

        if (latestMoodEntry
                .getEntryDate()
                .equals(LocalDate.now())) {

            todayRecommendation = recommendationRepository
                            .findFirstBySession_MoodEntry_IdOrderByRankNoAsc(
                                    latestMoodEntry.getId()
                            )
                            .map(
                                    HomeResponse.TodayRecommendation::from
                            )
                            .orElse(null);
        }

        return new HomeResponse(latestMood, todayRecommendation);
    }
}