package com.moodandmove.recommendation.controller;

import com.moodandmove.recommendation.domain.dto.response.ActionCategoryResponse;
import com.moodandmove.recommendation.service.ActionCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/action-categories")
@RequiredArgsConstructor
public class ActionCategoryController {

    private final ActionCategoryService actionCategoryService;

    @GetMapping
    public ResponseEntity<List<ActionCategoryResponse>> getCategories()
    {
        return ResponseEntity.ok(
                actionCategoryService.getAllCategories()
        );
    }

}
