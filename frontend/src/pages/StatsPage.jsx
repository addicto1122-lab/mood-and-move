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
<<<<<<< Updated upstream
  PieChart,
  Pie,
  Cell
} from "recharts";

/*
 * 감정별 색상
 */
const EMOTION_STYLE = {
  JOY: {
    color: "#F6C445",
    bg: "#FFF6D8",
    text: "#8A6200"
  },

  CALM: {
    color: "#7BC96F",
    bg: "#EAF7E7",
    text: "#2E6B2E"
  },

  NEUTRAL: {
    color: "#B8BDC7",
    bg: "#F3F4F6",
    text: "#4B5563"
  },

  SAD: {
    color: "#5B8DEF",
    bg: "#EAF1FF",
    text: "#244C9A"
  },

  ANXIOUS: {
    color: "#F39C4A",
    bg: "#FFF1E5",
    text: "#9A4F12"
  },

  ANGRY: {
    color: "#E85D5D",
    bg: "#FFEAEA",
    text: "#992B2B"
  }
};

/*
 * 도넛 조각 안에 Emoji 표시
 */
function renderEmotionLabel({
  cx,
  cy,
  midAngle,
  innerRadius,
  outerRadius,
  payload
}) {
  const RADIAN = Math.PI / 180;

  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;

  const x = cx + radius * Math.cos(-midAngle * RADIAN);

  const y = cy + radius * Math.sin(-midAngle * RADIAN);

  return (
    <text
      x={x}
      y={y}
      textAnchor="middle"
      dominantBaseline="central"
      fontSize="22"
    >
      {payload.emoji}
    </text>
  );
}
=======
} from "recharts";
import { authFetch } from "../api/authApi";
>>>>>>> Stashed changes

export default function StatsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [currentDate, setCurrentDate] = useState(() => {
    const queryYear = Number(searchParams.get("year"));
    const queryMonth = Number(searchParams.get("month"));

    if (queryYear && queryMonth >= 1 && queryMonth <= 12) {
      return new Date(queryYear, queryMonth - 1, 1);
    }
    return new Date();
  });

  /*
   * 행동 전/후 그래프용 감정 기록
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

  const [loading, setLoading] = useState(true);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth() + 1;

  const [activeTab, setActiveTab] = useState("SUMMARY");

  const [nearbyPlaces, setNearbyPlaces] = useState([]);
  const [placeType, setPlaceType] = useState("PARK");
  const [placeLoading, setPlaceLoading] = useState(false);
  const [placeError, setPlaceError] = useState("");

  const getCurrentLocation = (type) => {
    if (!navigator.geolocation) {
      console.log("현재 브라우저에서 위치 기능을 지원하지 않습니다.");
      return;
    }

    setPlaceLoading(true);
    setPlaceError("");

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const latitude = position.coords.latitude;

        const longitude = position.coords.longitude;

        console.log("현재 위도 =", latitude);
        console.log("현재 경도 =", longitude);

        try {
          const response = await fetch(
            `/api/places/nearby?type=${type}&latitude=${latitude}&longitude=${longitude}&radius=3000`,
            {
              method: "GET",
              credentials: "include"
            }
          );

          if (!response.ok) {
            throw new Error(`장소 조회 실패: ${response.status}`);
          }
          const data = await response.json();
          console.log(`${type} 검색 결과 =`, data);
          setNearbyPlaces(data);
          setPlaceType(type);
        } catch (error) {
          console.error(error);
          setPlaceError("주변 장소를 불러오지 못했습니다.");
        } finally {
          setPlaceLoading(false);
        }
      },
      (error) => {
        console.error("현재 위치 조회 실패 =", error);

        setPlaceError("현재 위치를 가져울 수 없습니다.");
        setPlaceLoading(false);
      }
    );
  };

  /*
   * =========================
   * 월별 Calendar 조회
   * =========================
   *
   * 현재는 기존 Calendar API 데이터를
   * 그래프에서도 사용한다.
   */
  useEffect(() => {
    async function fetchMoodEntries() {
      try {
        setLoading(true);

        const response = await authFetch(
          `/api/calendar?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include"
          }
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
<<<<<<< Updated upstream
   * Calendar 상세 조회
   * =========================
   */
  useEffect(() => {
    async function fetchActionEffects() {
      try {
        const response = await fetch(
          `/api/stats/monthly/actions?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include"
          }
        );

        if (!response.ok) {
          throw new Error("행동별 효과 통계 조회 실패");
        }
        const data = await response.json();

        setActionEffects(data);
      } catch (error) {
        console.error(error);
      }
    }
    fetchActionEffects();
  }, [year, month]);

  /*
   * =========================
   * Calendar 상세 조회
   * =========================
   */
  async function fetchCalendarDetail(moodEntryId) {
    try {
      const response = await fetch(`/api/calendar/${moodEntryId}`, {
        method: "GET",
        credentials: "include"
      });

      if (!response.ok) {
        throw new Error("일기 상세 조회 실패");
      }

      const data = await response.json();

      setSelectedEntry(data);
    } catch (error) {
      console.error(error);
    }
  }

  /*
   * =========================
=======
>>>>>>> Stashed changes
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
            credentials: "include"
          }
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

  const changeMonth = (newDate) => {
    setCurrentDate(newDate);

    setSearchParams({
      year: newDate.getFullYear(),
      month: newDate.getMonth() + 1,
    });
  };

  /*
   * =========================
   * 이전 달
   * =========================
   */
  const prevMonth = () => {
    changeMonth(new Date(year, month - 2, 1));
  };

  /*
   * =========================
   * 다음 달
   * =========================
   */
  const nextMonth = () => {
    changeMonth(new Date(year, month, 1));
  };

  /*
   * =========================
<<<<<<< Updated upstream
   * Calendar 생성
   * =========================
   */
  const daysInMonth = new Date(year, month, 0).getDate();

  const firstDay = new Date(year, month - 1, 1).getDay();

  const findEntry = (day) => {
    const date = `${year}-${String(month).padStart(2, "0")}-${String(
      day
    ).padStart(2, "0")}`;

    return days.find((entry) => entry.date === date);
  };

  const calendarCells = [];

  /*
   * 앞쪽 빈칸
   */
  for (let i = 0; i < firstDay; i++) {
    calendarCells.push(
      <div key={`empty-${i}`} className="calendar-cell empty" />
    );
  }

  /*
   * 날짜
   */
  for (let day = 1; day <= daysInMonth; day++) {
    const entry = findEntry(day);

    calendarCells.push(
      <div
        key={day}
        className={`calendar-cell ${entry ? "has-entry" : ""}`}
        onClick={() => {
          if (entry) {
            fetchCalendarDetail(entry.moodEntryId);
          }
        }}
      >
        <span className="day-number">{day}</span>

        {entry && (
          <div className="calendar-mood">
            <span className="calendar-emoji">{entry.emoji}</span>

            <span className="calendar-score">{entry.moodScore}점</span>
          </div>
        )}
      </div>
    );
  }

  /*
   * =========================
=======
>>>>>>> Stashed changes
   * 행동 전 / 후 그래프 데이터
   * =========================
   */
  const moodChartData = moodEntries.map((entry) => ({
    date: entry.date.substring(5),

    beforeScore: Number(entry.moodScore),

    afterScore: entry.afterScore == null ? null : Number(entry.afterScore),

    emotion: `${entry.emoji} ${entry.emotionName}`
  }));

  /*
   * 행동 Emoji
   */
<<<<<<< Updated upstream
  const emotionChartData =
    monthlyStats?.emotions?.map((emotion) => {
      const style = EMOTION_STYLE[emotion.emotionCode] || {
        color: "#B8BDC7",
        bg: "#F3F4F6",
        text: "#4B5563"
      };

      return {
        name: emotion.emotionName,

        emotionCode: emotion.emotionCode,

        emoji: emotion.emoji,

        value: emotion.count,

        rate: emotion.rate,

        fill: style.color,

        bg: style.bg,

        text: style.text
      };
    }) || [];

=======
>>>>>>> Stashed changes
  const ACTION_EMOJI = {
    1: "🚶",
    2: "🤸",
    3: "🎵",
    4: "🌿"
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

      {/* 통계 분할 페이지 */}
      <div className="stats-tabs">
        <button
          className={activeTab === "SUMMARY" ? "active" : ""}
          onClick={() => setActiveTab("SUMMARY")}
        >
          요약
        </button>

        <button
          className={activeTab === "TREND" ? "active" : ""}
          onClick={() => setActiveTab("TREND")}
        >
          기분 변화
        </button>

        <button
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

              {/* <div className="summary-card">
              <span>추천 행동</span>
              <strong>{monthlyStats.recommendationCount}회</strong>
            </div>
            <div className="summary-card">
              <span>행동 실행</span>
              <strong>{monthlyStats.executionCount}회</strong>
            </div>
            <div className="summary-card">
              <span>실행률</span>
              <strong>{monthlyStats.executionRate}%</strong>
            </div>
            <div className="summary-card">
              <span>긍정 변화율</span>
              <strong>{monthlyStats.positiveRate}%</strong>
            </div>
            <div className="summary-card">
              <span>평균 변화량</span>
              <strong>
                {monthlyStats.averageDelta > 0 ? "+" : ""}
                {monthlyStats.averageDelta}
              </strong>
            </div>
          </section>
          <div className="summary-links">
            <button type="button" onClick={() => setActiveTab("TREND")}>
              <span>기분 변화 자세히 보기</span>
              <span>→</span>
            </button>

<<<<<<< Updated upstream
      {/* =======================
          감정 분포 도넛
      ======================== */}

      {emotionChartData.length > 0 && (
        <section className="emotion-summary">
          <h2>{month}월 감정 분포</h2>

          <div className="emotion-donut-card">
            <div className="emotion-donut-chart">
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={emotionChartData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={68}
                    outerRadius={108}
                    paddingAngle={3}
                    labelLine={false}
                    label={renderEmotionLabel}
                  >
                    {emotionChartData.map((emotion) => (
                      <Cell key={emotion.emotionCode} fill={emotion.fill} />
                    ))}
                  </Pie>

                  <Tooltip
                    formatter={(value, name, props) => [
                      `${value}회 (${props.payload.rate}%)`,

                      `${props.payload.emoji} ${name}`
                    ]}
                  />
                </PieChart>
              </ResponsiveContainer>

              {/* 원 가운데 */}

              <div className="emotion-donut-center">
                <strong>{month}월</strong>

                <span>총 {monthlyStats.diaryCount}회</span>
              </div>
            </div>

            {/* 감정별 상세 */}

            <div className="emotion-legend-list">
              {emotionChartData.map((emotion) => (
                <div
                  key={emotion.emotionCode}
                  className="emotion-legend-item"
                  style={{
                    backgroundColor: emotion.bg,

                    borderLeft: `5px solid ${emotion.fill}`
                  }}
                >
                  <div className="emotion-legend-left">
                    <span className="emotion-legend-emoji">
                      {emotion.emoji}
                    </span>

                    <div>
                      <strong
                        style={{
                          color: emotion.text
                        }}
                      >
                        {emotion.name}
                      </strong>

                      <p>{emotion.value}회</p>
                    </div>
                  </div>

                  <span
                    className="emotion-legend-rate"
                    style={{
                      color: emotion.text
                    }}
                  >
                    {emotion.rate}%
                  </span>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      {actionEffects.length > 0 && (
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
                        width: `${Math.min(Number(action.positiveRate), 100)}%`
                      }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* =======================
          일기 상세
      ======================== */}

      {selectedEntry && (
        <section className="diary-detail">
          <div className="diary-detail-header">
            <div>
              <span className="detail-date">{selectedEntry.date}</span>
              <h2>
                {selectedEntry.emoji} {selectedEntry.emotionName}
              </h2>
            </div>
            <button
              className="detail-close"
              onClick={() => setSelectedEntry(null)}
            >
              ×
=======
            <button type="button" onClick={() => setActiveTab("ACTION")}>
              <span>행동 효과 자세히 보기</span>
              <span>→</span>
>>>>>>> Stashed changes
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
                    />

<<<<<<< Updated upstream
                <Line
                  type="monotone"
                  dataKey="beforeScore"
                  name="행동 전"
                  stroke="#3b82f6"
                  strokeWidth={3}
                  dot={{
                    r: 4,
                    strokeWidth: 3
                  }}
                  activeDot={{
                    r: 6
                  }}
                />
=======
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
>>>>>>> Stashed changes

      {/* =========================
          행동별 효과
      ========================= */}

<<<<<<< Updated upstream
                <Line
                  type="monotone"
                  dataKey="afterScore"
                  name="행동 후"
                  stroke="#22c55e"
                  strokeWidth={3}
                  dot={{
                    r: 4,
                    strokeWidth: 3
                  }}
                  activeDot={{
                    r: 6
                  }}
                  connectNulls={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
=======
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
                            width: `${Math.min(Number(action.positiveRate), 100)}%`,
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
>>>>>>> Stashed changes
      )}
      <div>
        <button onClick={() => getCurrentLocation("PARK")}>🌳 공원</button>
        <button onClick={() => getCurrentLocation("CAFE")}>☕ 카페</button>
        <button onClick={() => getCurrentLocation("SHOPPING")}>🛍 쇼핑</button>
        <button onClick={() => getCurrentLocation("LIBRARY")}>📚 도서관</button>
        <button onClick={() => getCurrentLocation("CINEMA")}>🎥 영화관</button>
      </div>
      {placeLoading && <p>주변 장소를 찾고 있습니다...</p>}
      {placeError && <p>{placeError}</p>}
      {nearbyPlaces.length > 0 && (
        <section>
          <h2>내 주변 장소</h2>
          {nearbyPlaces.map((place) => (
            <div key={place.placeId}>
              <h3>{place.name}</h3>
              <p>{place.category}</p>
              <p>{place.roadAddress || place.address}</p>
              <p>현재 위치에서 {place.distance}m</p>
              <a href={place.placeUrl} target="_blank" rel="noreferror">
                카카오맵에서 보기
              </a>
              <hr />
            </div>
          ))}
        </section>
      )}
    </main>
  );
}
