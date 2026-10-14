package com.moodandmove.recommendation.service;

import com.moodandmove.recommendation.domain.dto.response.ActionCategoryResponse;
import com.moodandmove.recommendation.repository.ActionCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionCategoryService {

    private final ActionCategoryRepository actionCategoryRepository;

    @Transactional(readOnly = true)
    public List<ActionCategoryResponse> getAllCategories()
    {
        return actionCategoryRepository
                .findAll()
                .stream()
                .map(category ->
                        new ActionCategoryResponse(
                                category.getCategoryCode(),
                                category.getCategoryName(),
                                category.getEmoji()
                        )
                )
                .toList();
    }

}
