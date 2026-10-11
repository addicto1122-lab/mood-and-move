package com.moodandmove.report.service;

import com.moodandmove.analysis.dto.*;
import com.moodandmove.analysis.service.UserActionStatService;
import com.moodandmove.report.dto.MonthlyReport;
import com.moodandmove.report.llm.MonthlyReportEncouragementGenerator;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final UserActionStatService userActionStatService;

    // 추가
    private final UserRepository userRepository;

    // 추가
    private final MonthlyReportEncouragementGenerator
            encouragementGenerator;


    public MonthlyReport create(
            Long userId,
            int year,
            int month
    ) {

        /*
         * 1. 사용자 조회
         * LLM 응원 문구에 닉네임 사용
         */
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );


        /*
         * 2. 월간 기본 통계
         */
        MonthlyStatsResponse stats =
                userActionStatService.getMonthlyStats(
                        userId,
                        year,
                        month
                );


        /*
         * 3. 월간 행동 효과 통계
         */
        MonthlyActionEffectResponse actions =
                userActionStatService.getMonthlyActionEffects(
                        userId,
                        year,
                        month
                );


        /*
         * 4. 가장 많이 기록한 감정
         */
        EmotionStatResponse topEmotion =
                stats.emotions()
                        .stream()
                        .max(
                                java.util.Comparator.comparingLong(
                                        EmotionStatResponse::count
                                )
                        )
                        .orElse(null);


        /*
         * 5. 가장 많이 실행한 카테고리
         *
         * executionRanking은 이미
         * 실행 횟수 내림차순 정렬되어 있으므로
         * 첫 번째 데이터를 사용
         */
        CategoryExecutionRankResponse topCategory =
                actions.executionRanking()
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * 6. 가장 효과가 좋았던 행동
         */
        BestActionResponse bestAction =
                actions.bestAction();


        /*
         * 7. 먼저 LLM 메시지가 없는
         * 기본 리포트를 생성
         */
        MonthlyReport baseReport =
                new MonthlyReport(
                        year,
                        month,

                        stats.diaryCount(),
                        stats.averageMoodScore(),

                        topEmotion == null
                                ? null
                                : topEmotion.emotionName(),

                        topEmotion == null
                                ? null
                                : topEmotion.emoji(),

                        stats.executionCount(),
                        stats.executionRate(),

                        stats.positiveRate(),
                        stats.averageDelta(),

                        bestAction == null
                                ? null
                                : bestAction.actionName(),

                        bestAction == null
                                ? null
                                : bestAction.delta(),

                        topCategory == null
                                ? null
                                : topCategory.category(),

                        topCategory == null
                                ? null
                                : topCategory.executionCount(),

                        // 아직 LLM 응원 문구 없음
                        null
                );


        /*
         * 8. LLM 응원의 한마디 생성
         *
         * Gemini 장애 때문에
         * 월간 메일 전체가 실패하면 안 되므로
         * 반드시 fallback을 둔다.
         */
        String encouragementMessage;

        if (stats.diaryCount() == 0) {

            /*
             * 기록이 하나도 없는 달은
             * 굳이 Gemini API를 호출하지 않음
             */
            encouragementMessage =
                    "지난달에는 기록이 없었어요. "
                            + "이번 달에는 부담 없이 지금의 마음을 "
                            + "한 번 기록해보는 것부터 시작해보세요. 🌱";

        } else {

            try {

                encouragementMessage =
                        encouragementGenerator.generate(
                                user.getNickname(),
                                baseReport
                        );

            } catch (Exception e) {

                /*
                 * Gemini 호출 실패 시 기본 문구
                 */
                System.err.println(
                        "월간 리포트 응원 문구 생성 실패"
                                + " userId="
                                + userId
                                + " / "
                                + e.getMessage()
                );

                encouragementMessage =
                        "이번 달의 기록도 소중한 변화의 한 부분이에요. "
                                + "다음 달에도 부담 없이 작은 기록을 이어가 보세요. 🌱";
            }
        }


        /*
         * 9. 응원 문구까지 포함한 최종 리포트 반환
         */
        return new MonthlyReport(
                year,
                month,

                stats.diaryCount(),
                stats.averageMoodScore(),

                topEmotion == null
                        ? null
                        : topEmotion.emotionName(),

                topEmotion == null
                        ? null
                        : topEmotion.emoji(),

                stats.executionCount(),
                stats.executionRate(),

                stats.positiveRate(),
                stats.averageDelta(),

                bestAction == null
                        ? null
                        : bestAction.actionName(),

                bestAction == null
                        ? null
                        : bestAction.delta(),

                topCategory == null
                        ? null
                        : topCategory.category(),

                topCategory == null
                        ? null
                        : topCategory.executionCount(),

                // ⭐ LLM이 생성한 문구
                encouragementMessage
        );
    }
}