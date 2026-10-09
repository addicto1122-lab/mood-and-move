package com.moodandmove.home.service;

import com.moodandmove.home.dto.response.HomeResponse;
import com.moodandmove.mood.repository.MoodEntryRepository;
import com.moodandmove.recommendation.domain.type.RecommendationStatus;
import com.moodandmove.recommendation.repository.RecommendationRepository;
import com.moodandmove.recommendation.service.ActionExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final MoodEntryRepository moodEntryRepository;
    private final RecommendationRepository recommendationRepository;
    private final ActionExecutionService actionExecutionService;

    @Transactional(readOnly = true)
    public HomeResponse getHome(Long userId) {
        // 1. 가장 최근의 진행 중 행동 조회
        var currentExecution = actionExecutionService
                .findCurrent(userId)
                .orElse(null);

        // 2. 삭제되지 않은 가장 최근 감정 기록 조회
        var latestMoodEntry = moodEntryRepository
                .findFirstByUser_IdAndDeletedAtIsNullOrderByEntryDateDescRecordedAtDesc(
                        userId
                )
                .orElse(null);

        if (latestMoodEntry == null) {
            return new HomeResponse(
                    null,
                    null,
                    currentExecution
            );
        }

        var latestMood = HomeResponse.LatestMood.from(latestMoodEntry);

        HomeResponse.TodayRecommendation todayRecommendation = null;

        // 3. 진행 중 행동이 없을 때만 오늘의 미선택 추천 조회
        if (currentExecution == null
                && latestMoodEntry.getEntryDate().equals(LocalDate.now())) {

            todayRecommendation = recommendationRepository
                    .findFirstBySession_MoodEntry_IdOrderByRankNoAsc(
                            latestMoodEntry.getId()
                    )
                    .filter(recommendation ->
                            recommendation.getStatus()
                                    == RecommendationStatus.PENDING)
                    .map(HomeResponse.TodayRecommendation::from)
                    .orElse(null);
        }

        // 4. 홈 화면에 필요한 데이터 반환
        return new HomeResponse(
                latestMood,
                todayRecommendation,
                currentExecution
        );
    }
}