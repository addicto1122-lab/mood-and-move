package com.moodandmove.recommendation.service;

import com.moodandmove.analysis.domain.entity.MoodRecheck;
import com.moodandmove.analysis.repository.MoodRecheckRepository;
import com.moodandmove.recommendation.domain.dto.response.ActionExecutionResponse;
import com.moodandmove.recommendation.domain.dto.response.RecommendationResponse;
import com.moodandmove.recommendation.domain.entity.ActionExecution;
import com.moodandmove.recommendation.domain.entity.Recommendation;
import com.moodandmove.recommendation.domain.entity.RecommendationSession;
import com.moodandmove.recommendation.domain.type.ExecutionStatus;
import com.moodandmove.recommendation.domain.type.RecommendationStatus;
import com.moodandmove.recommendation.repository.*;
import com.moodandmove.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.moodandmove.recommendation.domain.entity.ActionCategoryScore;
import com.moodandmove.analysis.service.UserActionStatService;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActionExecutionService {
    private final UserRepository userRepository;
    private final RecommendationSessionRepository recommendationSessionRepository;
    private final RecommendationRepository recommendationRepository;
    private final ActionExecutionRepository actionExecutionRepository;
    private final MoodRecheckRepository moodRecheckRepository;
    private final ActionCategoryScoreRepository actionCategoryScoreRepository;
    private final PersonalScoreCalculator personalScoreCalculator;
    private final UserActionStatService userActionStatService;
    private final ActionCategoryRepository actionCategoryRepository;


    //같은 사용자의 요청을 순서대로 처리
    private void lockUser(Long userId) {
        userRepository.findForUpdate(userId).orElseThrow(() -> notFound("사용자를 찾을 수 없습니다."));
    }

    //사용자 소유의 세션인지, 연결된 일기가 유효한지 확인
    private RecommendationSession ownedSession(Long userId, Long sessionId) {
        var session = recommendationSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> notFound("추천 세션을 찾을 수 없습니다."));
        var moodEntry = session.getMoodEntry();

        if(moodEntry.getDeletedAt() != null || !moodEntry.getUser().getId().equals(userId)) {
            throw notFound("감정기록을 찾을 수 없습니다.");
        }
        return session;
    }
    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
    @Transactional
    public ActionExecutionResponse select(
            Long userId,
            Long sessionId,
            Long recommendationId
    ) {
        // 1. 같은 사용자의 중복·동시 요청을 순서대로 처리
        lockUser(userId);

        // 2. 본인의 추천 세션인지 확인
        var session = ownedSession(userId, sessionId);

        // 3. 이미 선택한 실행이 있다면 확인
        var existing = actionExecutionRepository.findBySession_Id(sessionId);

        if (existing.isPresent()) {
            var execution = existing.get();

            if (!execution.getRecommendation().getId()
                    .equals(recommendationId)) {
                throw conflict("이미 다른 행동을 선택했습니다.");
            }

            // 같은 행동을 다시 선택한 요청은 기존 결과 반환
            var recheck = moodRecheckRepository
                    .findByActionExecution_Id(execution.getId())
                    .orElseThrow(() ->
                            conflict("연결된 재측정 기록이 없습니다."));

            return toResponse(execution, recheck);
        }

        // 4. 해당 세션의 추천 목록에서 선택한 추천 확인
        var candidates = recommendationRepository
                .findAllBySession_IdOrderByRankNoAsc(sessionId);

        var selected = candidates.stream()
                .filter(item -> item.getId().equals(recommendationId))
                .findFirst()
                .orElseThrow(() ->
                        notFound("이 추천 목록에 없는 행동입니다."));

        // 추천 3개가 모두 선택 대기 상태여야 함
        if (candidates.size() != 3
                || candidates.stream().anyMatch(item ->
                item.getStatus() != RecommendationStatus.PENDING)) {
            throw conflict("선택 가능한 추천 상태가 아닙니다.");
        }

        // 5. 선택한 추천은 SELECTED, 나머지는 UNSELECTED
        selected.select();

        candidates.stream()
                .filter(item -> !item.getId().equals(recommendationId))
                .forEach(Recommendation::unselect);

        // 6. 선택 즉시 실행 시작: STARTED와 startedAt 저장
        var execution = actionExecutionRepository.saveAndFlush(
                ActionExecution.select(userId, selected)
        );

        // 7. 재측정 전 점수만 담은 행 생성
        var recheck = moodRecheckRepository.save(
                MoodRecheck.prepare(
                        execution,
                        session.getMoodEntry().getMoodScore()
                )
        );

        return toResponse(execution, recheck);
    }

    @Transactional
    public RecommendationResponse skip(Long userId, Long sessionId) {
        // 1. 같은 사용자의 선택·건너뛰기 요청을 순서대로 처리
        lockUser(userId);

        // 2. 본인 소유의 유효한 추천 세션인지 확인
        ownedSession(userId, sessionId);

        // 3. 이미 행동을 선택했다면 전체 건너뛰기 불가
        if (actionExecutionRepository.findBySession_Id(sessionId).isPresent()) {
            throw conflict("행동을 선택한 뒤에는 추천 전체를 건너뛸 수 없습니다.");
        }

        var candidates = recommendationRepository
                .findAllBySession_IdOrderByRankNoAsc(sessionId);

        if (candidates.size() != 3) {
            throw conflict("추천 목록이 올바르지 않습니다.");
        }

        // 4. 이미 모두 건너뛴 경우에는 기존 결과 반환
        boolean alreadySkipped = candidates.stream()
                .allMatch(item ->
                        item.getStatus() == RecommendationStatus.SKIPPED);

        if (alreadySkipped) {
            return new RecommendationResponse(
                    sessionId,
                    candidates.stream()
                            .map(RecommendationResponse.Item::from)
                            .toList()
            );
        }

        // 5. 모두 대기 상태일 때만 건너뛰기 가능
        boolean hasNonPending = candidates.stream()
                .anyMatch(item ->
                        item.getStatus() != RecommendationStatus.PENDING);

        if (hasNonPending) {
            throw conflict("건너뛸 수 있는 추천 상태가 아닙니다.");
        }

        // 6. 추천 3개를 모두 SKIPPED로 변경
        candidates.forEach(Recommendation::skip);

        return new RecommendationResponse(
                sessionId,
                candidates.stream()
                        .map(RecommendationResponse.Item::from)
                        .toList()
        );
    }
    // 본인 소유의 실행 기록 조회
    @Transactional(readOnly = true)
    public ActionExecutionResponse get(Long userId, Long executionId) {
        var execution = ownedExecution(userId, executionId);

        return response(execution);
    }

    // 메인화면에 표시할 가장 최근 진행 중 행동 조회
    @Transactional(readOnly = true)
    public Optional<ActionExecutionResponse> findCurrent(Long userId) {
        return actionExecutionRepository
                .findFirstByUserIdAndStatusAndSession_MoodEntry_DeletedAtIsNullOrderByStartedAtDescIdDesc(
                        userId,
                        ExecutionStatus.STARTED
                )
                .map(this::response);
    }

    // 실행 기록의 소유권과 연결된 일기 상태 확인
    private ActionExecution ownedExecution(
            Long userId,
            Long executionId
    ) {
        var execution = actionExecutionRepository
                .findByIdAndUserId(executionId, userId)
                .orElseThrow(() ->
                        notFound("실행 기록을 찾을 수 없습니다."));

        ownedSession(userId, execution.getSession().getId());

        return execution;
    }

    // 실행 기록과 재측정 기록을 응답 DTO로 변환
    private ActionExecutionResponse response(ActionExecution execution) {
        var recheck = moodRecheckRepository
                .findByActionExecution_Id(execution.getId())
                .orElseThrow(() ->
                        conflict("연결된 재측정 기록이 없습니다."));

        return toResponse(execution, recheck);
    }

    // 사용자·카테고리별 재측정 결과를 집계하고 개인화 점수 갱신
    private void updateCategoryScore(Long userId, String category) {
        // 1. 완료된 재측정 기록 집계
        var summary = moodRecheckRepository.summarizeCategory(
                userId,
                category
        );

        if (summary.getSampleCount() == 0 || summary.getAvgDelta() == null) {
            throw conflict("점수를 계산할 재측정 완료 기록이 없습니다.");
        }

        // 2. 평균 감정 변화량을 소수점 둘째 자리까지 계산
        var avgDelta = summary.getAvgDelta()
                .setScale(2, RoundingMode.HALF_UP);

        // 3. 기존 계산식으로 개인화 점수 계산
        var personalScore = personalScoreCalculator.calculate(
                avgDelta,
                summary.getPositiveCount(),
                summary.getSampleCount()
        );

        // 4. 기존 카테고리 통계 조회, 없으면 새 객체 생성
        var categoryScore = actionCategoryScoreRepository
                .findByUserIdAndCategory(userId, category)
                .orElseGet(() ->
                        ActionCategoryScore.create(userId, category));

        // 5. 집계한 값으로 통계 갱신
        categoryScore.updateStatistics(
                personalScore,
                avgDelta,
                Math.toIntExact(summary.getPositiveCount()),
                Math.toIntExact(summary.getSampleCount())
        );

        // 6. 저장
        actionCategoryScoreRepository.save(categoryScore);
    }

    @Transactional
    public ActionExecutionResponse recheck(
            Long userId,
            Long executionId,
            Integer afterScore
    ) {
        // 1. 슬라이더로 전달받은 기분 점수 검증
        if (afterScore == null || afterScore < 1 || afterScore > 60) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "기분 점수는 1~60 사이여야 합니다."
            );
        }

        // 2. 같은 사용자의 중복·동시 요청을 순서대로 처리
        lockUser(userId);

        // 3. 본인의 실행 기록과 연결된 재측정 기록 조회
        var execution = ownedExecution(userId, executionId);

        var recheck = moodRecheckRepository
                .findByActionExecution_Id(executionId)
                .orElseThrow(() ->
                        conflict("연결된 재측정 기록이 없습니다."));

        // 4. 이미 완료했다면 최초 결과 반환
        if (execution.getStatus() == ExecutionStatus.COMPLETED) {
            return toResponse(execution, recheck);
        }

        // 5. 진행 중인 실행만 재측정 가능
        if (execution.getStatus() != ExecutionStatus.STARTED
                || execution.getStartedAt() == null) {
            throw conflict("진행 중인 행동만 재측정할 수 있습니다.");
        }

        var recommendation = execution.getRecommendation();

        if (recommendation.getStatus() != RecommendationStatus.SELECTED
                || recommendation.isRecheckCompleted()
                || recheck.getAfterScore() != null
                || recheck.getCheckedAt() != null) {
            throw conflict("재측정 상태를 확인해 주세요.");
        }

        // 6. 전달받은 점수로 재측정·실행 완료 처리
        var now = LocalDateTime.now();

        recheck.complete(afterScore, now);
        execution.complete(now);
        recommendation.completeRecheck();

        // 7. 이번 결과까지 집계되도록 DB에 변경 사항 반영
        moodRecheckRepository.flush();

        // 8. 사용자·카테고리별 통계와 개인화 점수 갱신
        // 사용자·카테고리별 개인화 통계 갱신
        updateCategoryScore(
                userId,
                recommendation.getAction().getCategory()
        );

        // 기존 월별 통계에 완료 결과 반영
        var moodEntry = execution.getSession().getMoodEntry();
        Long actionId = recommendation.getAction().getId();

        // 최초 일기에 기록된 감정과 날짜 기준으로 집계
        String emotionCode = moodEntry.getEmotion().getEmotionCode();
        var entryDate = moodEntry.getEntryDate();

        userActionStatService.recordExecution(
                userId,
                actionId,
                emotionCode,
                entryDate
        );

        userActionStatService.recordRecheck(
                userId,
                actionId,
                emotionCode,
                entryDate,
                afterScore - recheck.getBeforeScore()
        );

        return toResponse(execution, recheck);
    }

    // 카테고리 정보까지 포함한 실행 응답 생성
    private ActionExecutionResponse toResponse(
            ActionExecution execution,
            MoodRecheck recheck
    ) {
        String categoryCode = execution.getRecommendation()
                .getAction()
                .getCategory();

        var actionCategory = actionCategoryRepository
                .findById(categoryCode)
                .orElse(null);

        return ActionExecutionResponse.from(
                execution,
                recheck,
                actionCategory
        );
    }
}
