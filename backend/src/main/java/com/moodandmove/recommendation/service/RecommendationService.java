//package com.moodandmove.recommendation.service;
//
//import com.moodandmove.recommendation.domain.entity.Action;
//import com.moodandmove.recommendation.repository.ActionHobbyRepository;
//import com.moodandmove.recommendation.repository.ActionRepository;
//import com.moodandmove.recommendation.repository.RecommendationRepository;
//import com.moodandmove.recommendation.repository.RecommendationSessionRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class RecommendationService {
//    private final ActionRepository actionRepository;
//    private final ActionHobbyRepository actionHobbyRepository;
//    private final RecommendationSessionRepository recommendationSessionRepository;
//    private final RecommendationRepository recommendationRepository;
//
//    public List<Action> getActiveActions() {
//        return actionRepository.findByActiveTrue();
//    }
//}
