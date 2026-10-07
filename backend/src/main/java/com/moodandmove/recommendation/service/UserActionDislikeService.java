package com.moodandmove.recommendation.service;

import com.moodandmove.recommendation.domain.dto.response.DislikeActionResponse;
import com.moodandmove.recommendation.domain.entity.Action;
import com.moodandmove.recommendation.domain.entity.UserActionDislike;
import com.moodandmove.recommendation.repository.ActionRepository;
import com.moodandmove.recommendation.repository.UserActionDislikeRepository;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActionDislikeService {

    private final UserRepository userRepository;
    private final ActionRepository actionRepository;
    private final UserActionDislikeRepository userActionDislikeRepository;

    @Transactional(readOnly = true)
    public List<DislikeActionResponse> getDislikeActions(Long userId) {

        Set<Long> dislikedActionIds =
                userActionDislikeRepository
                        .findAllByUser_Id(userId)
                        .stream()
                        .map(dislike -> dislike.getAction().getId())
                        .collect(Collectors.toSet());

        return actionRepository
                .findAllByActiveTrue()
                .stream()
                .sorted(Comparator.comparing(Action::getId))
                .map(action ->
                        new DislikeActionResponse(
                                action.getId(),
                                action.getName(),
                                action.getCategory(),
                                action.getDurationMinutes(),
                                dislikedActionIds.contains(action.getId())
                        )
                )
                .toList();
    }

    @Transactional
    public void addDislike(
            Long userId,
            Long actionId
    ) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        Action action = actionRepository
                .findById(actionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "행동을 찾을 수 없습니다."
                        )
                );

        if (!action.isActive()) {
            throw new IllegalArgumentException(
                    "현재 사용할 수 없는 행동입니다."
            );
        }

        boolean alreadyExists =
                userActionDislikeRepository
                        .existsByUser_IdAndAction_Id(
                                userId,
                                actionId
                        );

        if (alreadyExists) {
            return;
        }

        UserActionDislike dislike =
                UserActionDislike.create(
                        user,
                        action
                );

        userActionDislikeRepository.save(dislike);
    }

    @Transactional
    public void removeDislike(
            Long userId,
            Long actionId
    ) {

        boolean exists =
                userActionDislikeRepository
                        .existsByUser_IdAndAction_Id(
                                userId,
                                actionId
                        );

        if (!exists) {
            return;
        }

        userActionDislikeRepository
                .deleteByUser_IdAndAction_Id(
                        userId,
                        actionId
                );
    }
}