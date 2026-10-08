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
  const [actionEffects, setActionEffects] = useState([]);

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

        setActionEffects(data || []);
      } catch (error) {
        console.error(error);
        setActionEffects([]);
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

  /*
   * =========================
   * 행동 전 / 후 그래프 데이터
   * =========================
   */
  const moodChartData = moodEntries.map((entry) => ({
    date: entry.date.substring(5),

    beforeScore: Number(entry.moodScore),

    afterScore: entry.afterScore == null ? null : Number(entry.afterScore),

    emotion: `${entry.emoji} ${entry.emotionName}`,
  }));

  /*
   * 행동 Emoji
   */
  const ACTION_EMOJI = {
    1: "🚶",
    2: "🤸",
    3: "🎵",
    4: "🌿",
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

                    <XAxis dataKey="date" />

                    <YAxis domain={[0, 60]} ticks={[0, 15, 30, 45, 60]} />

                    <Tooltip
                      formatter={(value, name) => [`${value}점`, name]}
                    />

                    <Legend />

                    <Line
                      type="monotone"
                      dataKey="beforeScore"
                      name="행동 전"
                      stroke="#3b82f6"
                      strokeWidth={3}
                      dot={{
                        r: 4,
                        strokeWidth: 3,
                      }}
                      activeDot={{
                        r: 6,
                      }}
                    />

                    <Line
                      type="monotone"
                      dataKey="afterScore"
                      name="행동 후"
                      stroke="#22c55e"
                      strokeWidth={3}
                      connectNulls={false}
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
          {actionEffects.length > 0 ? (
            <section className="action-effect-section">
              <h2>{month}월 행동별 효과</h2>

              <p className="action-effect-description">
                이번 달 실행한 행동이 기분에 어떤 변화를 주었는지 확인해보세요.
              </p>

              <div className="action-effect-list">
                {actionEffects.map((action) => (
                  <div key={action.actionId} className="action-effect-card">
                    <div className="action-effect-header">
                      <div className="action-effect-title">
                        <span className="action-effect-emoji">
                          {ACTION_EMOJI[action.actionId] || "✨"}
                        </span>

                        <div>
                          <strong>{action.actionName}</strong>

                          <span>실행 {action.executionCount}회</span>
                        </div>
                      </div>

                      <div
                        className={
                          action.averageDelta > 0
                            ? "delta positive"
                            : action.averageDelta < 0
                              ? "delta negative"
                              : "delta neutral"
                        }
                      >
                        {action.averageDelta > 0 ? "+" : ""}
                        {action.averageDelta}
                      </div>
                    </div>

                    <div className="action-effect-stats">
                      <div>
                        <span>추천</span>

                        <strong>{action.recommendationCount}회</strong>
                      </div>

                      <div>
                        <span>실행</span>

                        <strong>{action.executionCount}회</strong>
                      </div>

                      <div>
                        <span>재측정</span>

                        <strong>{action.sampleCount}회</strong>
                      </div>
                    </div>

                    <div className="positive-rate-area">
                      <div className="positive-rate-header">
                        <span>긍정 변화율</span>

                        <strong>{action.positiveRate}%</strong>
                      </div>

                      <div className="positive-rate-bar">
                        <div
                          className="positive-rate-fill"
                          style={{
                            width: `${Math.min(
                              Number(action.positiveRate),
                              100,
                            )}%`,
                          }}
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          ) : (
            <section className="stats-empty action-empty">
              <div className="empty-icon">🌱</div>

              <strong>아직 행동 효과 데이터가 없어요</strong>

              <p>
                추천 행동을 실행하고 기분을 다시 기록하면
                <br />
                어떤 행동이 도움이 되었는지 보여드릴게요.
              </p>
            </section>
          )}
        </>
      )}
    </main>
  );
}
