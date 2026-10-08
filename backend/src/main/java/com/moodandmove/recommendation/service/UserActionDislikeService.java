
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

    // 전체 행동 및 사용자 비선호 여부 조회
    @Transactional(readOnly = true)
    public List<DislikeActionResponse> getDislikeActions(Long userId) {

        Set<Long> dislikedActionIds =
                userActionDislikeRepository
                        .findAllByUser_Id(userId)
                        .stream()
                        .map(dislike -> dislike.getAction().getId())
                        .collect(Collectors.toSet());

        return actionRepository
                .findAll()
                .stream()
                .sorted(Comparator.comparing(Action::getId))
                .map(action ->
                        new DislikeActionResponse(
                                action.getId(),
                                action.getActionName(),
                                action.getCategory(),
                                action.getDurationMinutes(),
                                dislikedActionIds.contains(action.getId())
                        )
                )
                .toList();
    }

    // 비선호 행동 등록
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

        // V9에서 active 컬럼 삭제
        // 기존 action.isActive() 검증 제거

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

    // 비선호 행동 해제
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
