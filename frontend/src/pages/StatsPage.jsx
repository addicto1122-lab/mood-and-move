import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import "./StatsPage.css";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";

import { authFetch } from "../api/authApi";

function MoodChartTooltip({ active, payload }) {
  if (!active || !payload?.length) {
    return null;
  }

  const data = payload[0]?.payload;

  return (
    <div className="mood-chart-tooltip">
      <strong>{data.fullDate}</strong>

      {data.beforeScore != null && <p>행동 전: {data.beforeScore}점</p>}

      {data.skipped ? (
        <p>추천 행동: 건너뜀</p>
      ) : data.afterScore != null ? (
        <p>행동 후: {data.afterScore}점</p>
      ) : (
        <p>행동 후: 기록 없음</p>
      )}
    </div>
  );
}

export default function StatsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  /*
   * =========================
   * 현재 조회 중인 년 / 월
   * =========================
   */
  const [currentDate, setCurrentDate] = useState(() => {
    const queryYear = Number(searchParams.get("year"));
    const queryMonth = Number(searchParams.get("month"));

    if (queryYear && queryMonth >= 1 && queryMonth <= 12) {
      return new Date(queryYear, queryMonth - 1, 1);
    }

    return new Date();
  });

  /*
   * 행동 전 / 후 그래프용 감정 기록
   */
  const [moodEntries, setMoodEntries] = useState([]);

  /*
   * 월간 통계
   */
  const [monthlyStats, setMonthlyStats] = useState(null);

  /*
   * 행동별 효과
   */
  const [actionEffects, setActionEffects] = useState({
    bestAction: null,
    executionRanking: [],
    effectRanking: [],
  });

  /*
   * 로딩
   */
  const [loading, setLoading] = useState(true);

  /*
   * 현재 선택된 통계 탭
   * SUMMARY / TREND / ACTION
   */
  const [activeTab, setActiveTab] = useState("SUMMARY");

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth() + 1;

  /*
   * =========================
   * 월별 감정 기록 조회
   * =========================
   *
   * Calendar API 데이터를
   * 행동 전 / 후 그래프에서도 사용한다.
   */
  useEffect(() => {
    async function fetchMoodEntries() {
      try {
        setLoading(true);

        const response = await authFetch(
          `/api/calendar?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("감정 기록 조회 실패");
        }

        const data = await response.json();

        setMoodEntries(data.days || []);
      } catch (error) {
        console.error(error);
        setMoodEntries([]);
      } finally {
        setLoading(false);
      }
    }

    fetchMoodEntries();
  }, [year, month]);

  /*
   * =========================
   * 월간 통계 조회
   * =========================
   */
  useEffect(() => {
    async function fetchMonthlyStats() {
      try {
        const response = await authFetch(
          `/api/stats/monthly?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("월간 통계 조회 실패");
        }

        const data = await response.json();

        setMonthlyStats(data);
      } catch (error) {
        console.error(error);
        setMonthlyStats(null);
      }
    }

    fetchMonthlyStats();
  }, [year, month]);

  /*
   * =========================
   * 행동별 효과 통계 조회
   * =========================
   */
  useEffect(() => {
    async function fetchActionEffects() {
      try {
        const response = await authFetch(
          `/api/stats/monthly/actions?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("행동별 효과 통계 조회 실패");
        }

        const data = await response.json();

        setActionEffects({
          bestAction: data.bestAction ?? null,
          executionRanking: data.executionRanking ?? [],
          effectRanking: data.effectRanking ?? [],
        });
      } catch (error) {
        console.error(error);
        setActionEffects({
          bestAction: null,
          executionRanking: [],
          effectRanking: [],
        });
      }
    }

    fetchActionEffects();
  }, [year, month]);

  /*
   * =========================
   * 월 변경
   * =========================
   */
  const changeMonth = (newDate) => {
    setCurrentDate(newDate);

    setSearchParams({
      year: newDate.getFullYear(),
      month: newDate.getMonth() + 1,
    });
  };

  /*
   * 이전 달
   */
  const prevMonth = () => {
    changeMonth(new Date(year, month - 2, 1));
  };

  /*
   * 다음 달
   */
  const nextMonth = () => {
    changeMonth(new Date(year, month, 1));
  };

  const today = new Date();

  const daysInMonth = new Date(year, month, 0).getDate();

  const isCurrentMonth =
    year === today.getFullYear() && month === today.getMonth() + 1;

  const chartEndDay = isCurrentMonth ? today.getDate() : daysInMonth;

  /*
   * 날짜로 빠르게 찾기 위한 Map
   *
   * 예:
   * "2026-10-10" -> 해당 일기
   */
  const moodEntryMap = new Map(moodEntries.map((entry) => [entry.date, entry]));

  /*
   * 1일부터 해당 월의 마지막 날까지 생성
   */
  const moodChartData = Array.from({ length: chartEndDay }, (_, index) => {
    const day = index + 1;

    const dateKey = [
      year,
      String(month).padStart(2, "0"),
      String(day).padStart(2, "0"),
    ].join("-");

    const entry = moodEntryMap.get(dateKey);

    return {
      day,
      fullDate: dateKey,

      beforeScore: entry ? Number(entry.moodScore) : null,

      afterScore: entry?.afterScore == null ? null : Number(entry.afterScore),

      skipped: entry?.skipped ?? false,

      emotion: entry ? `${entry.emoji} ${entry.emotionName}` : null,
    };
  });

  const xAxisTicks = [];

  for (let day = 1; day <= chartEndDay; day += 5) {
    xAxisTicks.push(day);
  }

  if (!xAxisTicks.includes(chartEndDay)) {
    xAxisTicks.push(chartEndDay);
  }

  const CATEGORY_LABEL = {
    WALK: "산책",
    SOCIAL: "소셜 활동",
    EATING: "먹기",
    EXERCISE: "운동",
    REST: "휴식",
    MUSIC: "음악",
    STUDY: "공부",
  };

  const CATEGORY_EMOJI = {
    WALK: "🚶",
    SOCIAL: "💬",
    EATING: "☕",
    EXERCISE: "🏃",
    REST: "🌿",
    MUSIC: "🎵",
    STUDY: "📚",
  };

  return (
    <main className="stats-page">
      {/* =========================
          Header
      ========================= */}

      <header className="stats-header">
        <div>
          <span className="stats-eyebrow">MOOD REPORT</span>

          <h1>나의 마음 통계</h1>

          <p>기록과 행동을 통해 달라진 마음을 확인해보세요.</p>
        </div>
      </header>

      {/* =========================
          월 선택
      ========================= */}

      <section className="stats-month-selector">
        <button type="button" onClick={prevMonth}>
          ‹
        </button>

        <h2>
          {year}년 {month}월
        </h2>

        <button type="button" onClick={nextMonth}>
          ›
        </button>
      </section>

      {/* =========================
          통계 탭
      ========================= */}

      <div className="stats-tabs">
        <button
          type="button"
          className={activeTab === "SUMMARY" ? "active" : ""}
          onClick={() => setActiveTab("SUMMARY")}
        >
          요약
        </button>

        <button
          type="button"
          className={activeTab === "TREND" ? "active" : ""}
          onClick={() => setActiveTab("TREND")}
        >
          기분 변화
        </button>

        <button
          type="button"
          className={activeTab === "ACTION" ? "active" : ""}
          onClick={() => setActiveTab("ACTION")}
        >
          행동 효과
        </button>
      </div>

      {/* =========================
          월간 요약
      ========================= */}

      {activeTab === "SUMMARY" && monthlyStats && (
        <>
          <section className="monthly-summary">
            <h2>{month}월 요약</h2>

            <div className="summary-grid">
              <div className="summary-card">
                <span>작성한 일기</span>

                <strong>{monthlyStats.diaryCount}일</strong>
              </div>

              <div className="summary-card">
                <span>평균 기분 점수</span>

                <strong>{monthlyStats.averageMoodScore}점</strong>
              </div>

              <div className="summary-card">
                <span>평균 변화량</span>

                <strong>
                  {monthlyStats.averageDelta > 0 ? "+" : ""}
                  {monthlyStats.averageDelta}점
                </strong>
              </div>
            </div>
          </section>

          <div className="summary-links">
            <button type="button" onClick={() => setActiveTab("TREND")}>
              <span>기분 변화 자세히 보기</span>

              <span>→</span>
            </button>

            <button type="button" onClick={() => setActiveTab("ACTION")}>
              <span>행동 효과 자세히 보기</span>

              <span>→</span>
            </button>
          </div>
        </>
      )}

      {/* =========================
          기분 변화
      ========================= */}

      {activeTab === "TREND" && (
        <>
          {loading ? (
            <p className="stats-loading">통계를 불러오는 중...</p>
          ) : moodChartData.length > 0 ? (
            <section className="mood-chart-section">
              <h2>{month}월 행동 전·후 기분 변화</h2>

              <div className="mood-chart">
                <ResponsiveContainer width="100%" height={280}>
                  <LineChart data={moodChartData}>
                    <CartesianGrid strokeDasharray="3 3" />

                    <XAxis
                      dataKey="day"
                      ticks={xAxisTicks}
                      tickFormatter={(day) => `${day}일`}
                      tick={{ fontSize: 10 }}
                    />

                    <YAxis domain={[0, 60]} ticks={[0, 15, 30, 45, 60]} />

                    <Tooltip content={<MoodChartTooltip />} />

                    <Legend />

                    <Line
                      type="monotone"
                      dataKey="beforeScore"
                      name="행동 전"
                      stroke="#3b82f6"
                      strokeWidth={3}
                      connectNulls={true}
                    />

                    <Line
                      type="monotone"
                      dataKey="afterScore"
                      name="행동 후"
                      stroke="#22c55e"
                      strokeWidth={3}
                      connectNulls={true}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </section>
          ) : (
            <section className="stats-empty">
              이번 달에는 기분 변화 데이터가 없습니다.
            </section>
          )}
        </>
      )}

      {/* =========================
          행동별 효과
      ========================= */}

      {activeTab === "ACTION" && (
        <>
          {actionEffects.bestAction ||
          actionEffects.executionRanking.length > 0 ||
          actionEffects.effectRanking.length > 0 ? (
            <section className="action-effect-section">
              <h2>{month}월 행동 효과</h2>

              <p className="action-effect-description">
                이번 달 어떤 행동이 마음에 도움이 되었는지 확인해보세요.
              </p>

              {/* =========================
            최고 효과 행동
        ========================= */}
              {actionEffects.bestAction && (
                <div className="best-action-card">
                  <div className="best-action-badge">
                    🏆 가장 효과가 좋았던 행동
                  </div>

                  <div className="best-action-content">
                    <div>
                      <span className="best-action-category">
                        {CATEGORY_EMOJI[actionEffects.bestAction.category] ||
                          "✨"}{" "}
                        {CATEGORY_LABEL[actionEffects.bestAction.category] ||
                          actionEffects.bestAction.category}
                      </span>

                      <strong>{actionEffects.bestAction.actionName}</strong>
                    </div>

                    <div
                      className={
                        actionEffects.bestAction.delta > 0
                          ? "best-action-delta positive"
                          : actionEffects.bestAction.delta < 0
                            ? "best-action-delta negative"
                            : "best-action-delta neutral"
                      }
                    >
                      {actionEffects.bestAction.delta > 0 ? "+" : ""}
                      {actionEffects.bestAction.delta}점
                    </div>
                  </div>
                </div>
              )}

              {/* =========================
            실행 횟수 랭킹
        ========================= */}
              <div className="ranking-section">
                <div className="ranking-header">
                  <div>
                    <span className="ranking-eyebrow">MOST ACTIVE</span>
                    <h3>많이 실행한 활동</h3>
                  </div>
                </div>

                <div className="ranking-list">
                  {actionEffects.executionRanking
                    .slice(0, 3)
                    .map((item, index) => (
                      <div key={item.category} className="ranking-item">
                        <span className="ranking-number">{index + 1}</span>

                        <span className="ranking-category-emoji">
                          {CATEGORY_EMOJI[item.category] || "✨"}
                        </span>

                        <div className="ranking-name">
                          <strong>
                            {CATEGORY_LABEL[item.category] || item.category}
                          </strong>
                          <span>{item.category}</span>
                        </div>

                        <strong className="ranking-value">
                          {item.executionCount}회
                        </strong>
                      </div>
                    ))}
                </div>
              </div>

              {/* =========================
            효과 랭킹
        ========================= */}
              <div className="ranking-section">
                <div className="ranking-header">
                  <div>
                    <span className="ranking-eyebrow">BEST EFFECT</span>
                    <h3>효과가 좋았던 활동</h3>
                  </div>
                </div>

                <div className="ranking-list">
                  {actionEffects.effectRanking
                    .slice(0, 3)
                    .map((item, index) => (
                      <div key={item.category} className="ranking-item">
                        <span className="ranking-number">{index + 1}</span>

                        <span className="ranking-category-emoji">
                          {CATEGORY_EMOJI[item.category] || "✨"}
                        </span>

                        <div className="ranking-name">
                          <strong>
                            {CATEGORY_LABEL[item.category] || item.category}
                          </strong>

                          <span>재측정 {item.sampleCount}회</span>
                        </div>

                        <strong
                          className={
                            item.averageDelta > 0
                              ? "ranking-value positive"
                              : item.averageDelta < 0
                                ? "ranking-value negative"
                                : "ranking-value"
                          }
                        >
                          {item.averageDelta > 0 ? "+" : ""}
                          {item.averageDelta}점
                        </strong>
                      </div>
                    ))}
                </div>
              </div>
            </section>
          ) : (
            <section className="stats-empty action-empty">
              <div className="empty-icon">🌱</div>

              <strong>아직 행동 효과 데이터가 없어요</strong>

              <p>
                추천 행동을 실행하고 기분을 다시 기록하면
                <br />
                어떤 활동이 도움이 되었는지 보여드릴게요.
              </p>
            </section>
          )}
        </>
      )}
    </main>
  );
}
