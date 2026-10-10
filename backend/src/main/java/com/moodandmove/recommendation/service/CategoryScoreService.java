package com.moodandmove.recommendation.service;

import com.moodandmove.recommendation.domain.dto.CategoryScoreDto;
import com.moodandmove.recommendation.domain.entity.ActionCategoryScore;
import com.moodandmove.recommendation.repository.ActionCategoryRepository;
import com.moodandmove.recommendation.repository.ActionCategoryScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryScoreService {

    private final ActionCategoryRepository actionCategoryRepository;
    private final ActionCategoryScoreRepository actionCategoryScoreRepository;

    @Transactional(readOnly = true)
    public List<CategoryScoreDto> getScores(Long userId) {
        // 사용자의 기존 카테고리 점수를 코드별로 정리
        Map<String, ActionCategoryScore> scoreMap =
                actionCategoryScoreRepository.findAllByUserId(userId)
                        .stream()
                        .collect(Collectors.toMap(
                                ActionCategoryScore::getCategory,
                                Function.identity()
                        ));

        // DB에 등록된 전체 카테고리에 사용자 점수 연결
        return actionCategoryRepository.findAll()
                .stream()
                .map(category -> {
                    String code = category.getCategoryCode();
                    ActionCategoryScore score = scoreMap.get(code);

                    // 재측정 기록이 없는 카테고리는 기본값 사용
                    if (score == null) {
                        return new CategoryScoreDto(
                                code,
                                new BigDecimal("50.00"),
                                0
                        );
                    }

                    return new CategoryScoreDto(
                            code,
                            score.getPersonalScore(),
                            score.getSampleCount()
                    );
                })
                .sorted(Comparator.comparing(CategoryScoreDto::category))
                .toList();
    }
}