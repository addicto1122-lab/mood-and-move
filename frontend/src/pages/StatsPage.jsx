import { useEffect, useState } from "react";
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
  PieChart,
  Pie,
  Cell,
} from "recharts";

/*
 * 감정별 색상
 */
const EMOTION_STYLE = {
  JOY: {
    color: "#F6C445",
    bg: "#FFF6D8",
    text: "#8A6200",
  },

  CALM: {
    color: "#7BC96F",
    bg: "#EAF7E7",
    text: "#2E6B2E",
  },

  NEUTRAL: {
    color: "#B8BDC7",
    bg: "#F3F4F6",
    text: "#4B5563",
  },

  SAD: {
    color: "#5B8DEF",
    bg: "#EAF1FF",
    text: "#244C9A",
  },

  ANXIOUS: {
    color: "#F39C4A",
    bg: "#FFF1E5",
    text: "#9A4F12",
  },

  ANGRY: {
    color: "#E85D5D",
    bg: "#FFEAEA",
    text: "#992B2B",
  },
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
  payload,
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

export default function StatsPage() {
  /*
   * 현재 조회 월
   */
  const [currentDate, setCurrentDate] = useState(new Date());

  /*
   * Calendar
   */
  const [days, setDays] = useState([]);

  const [loading, setLoading] = useState(true);

  /*
   * 일기 상세
   */
  const [selectedEntry, setSelectedEntry] = useState(null);

  /*
   * 월 통계
   */
  const [monthlyStats, setMonthlyStats] = useState(null);

  const year = currentDate.getFullYear();

  const month = currentDate.getMonth() + 1;

  /*
   * =========================
   * 월별 Calendar 조회
   * =========================
   */
  useEffect(() => {
    async function fetchCalendar() {
      try {
        setLoading(true);

        const response = await fetch(
          `/api/calendar?year=${year}&month=${month}`,
          {
            method: "GET",
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("캘린더 조회 실패");
        }

        const data = await response.json();

        setDays(data.days);
      } catch (error) {
        console.error(error);
      } finally {
        setLoading(false);
      }
    }

    fetchCalendar();
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
        credentials: "include",
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
   * 월간 통계 조회
   * =========================
   */
  useEffect(() => {
    async function fetchMonthlyStats() {
      try {
        const response = await fetch(
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
      }
    }

    fetchMonthlyStats();
  }, [year, month]);

  /*
   * =========================
   * 이전 달
   * =========================
   */
  const prevMonth = () => {
    setCurrentDate(new Date(year, month - 2, 1));

    setSelectedEntry(null);
  };

  /*
   * =========================
   * 다음 달
   * =========================
   */
  const nextMonth = () => {
    setCurrentDate(new Date(year, month, 1));

    setSelectedEntry(null);
  };

  /*
   * =========================
   * Calendar 생성
   * =========================
   */
  const daysInMonth = new Date(year, month, 0).getDate();

  const firstDay = new Date(year, month - 1, 1).getDay();

  const findEntry = (day) => {
    const date = `${year}-${String(month).padStart(2, "0")}-${String(
      day,
    ).padStart(2, "0")}`;

    return days.find((entry) => entry.date === date);
  };

  const calendarCells = [];

  /*
   * 앞쪽 빈칸
   */
  for (let i = 0; i < firstDay; i++) {
    calendarCells.push(
      <div key={`empty-${i}`} className="calendar-cell empty" />,
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
      </div>,
    );
  }

  /*
   * =========================
   * 행동 전 / 후 그래프 데이터
   * =========================
   */
  const moodChartData = days.map((entry) => ({
    date: entry.date.substring(5),

    beforeScore: Number(entry.moodScore),

    afterScore: entry.afterScore == null ? null : Number(entry.afterScore),

    emotion: `${entry.emoji} ${entry.emotionName}`,
  }));

  /*
   * =========================
   * 감정 도넛 그래프 데이터
   * =========================
   */
  const emotionChartData =
    monthlyStats?.emotions?.map((emotion) => {
      const style = EMOTION_STYLE[emotion.emotionCode] || {
        color: "#B8BDC7",
        bg: "#F3F4F6",
        text: "#4B5563",
      };

      return {
        name: emotion.emotionName,

        emotionCode: emotion.emotionCode,

        emoji: emotion.emoji,

        value: emotion.count,

        rate: emotion.rate,

        fill: style.color,

        bg: style.bg,

        text: style.text,
      };
    }) || [];

  return (
    <main className="stats-page">
      {/* =======================
          Header
      ======================== */}

      <header className="stats-header">
        <div>
          <span className="stats-eyebrow">MONTHLY MOOD</span>

          <h1>나의 감정 기록</h1>

          <p>한 달 동안의 마음을 한눈에 확인해보세요.</p>
        </div>
      </header>

      {/* =======================
          Calendar
      ======================== */}

      <section className="calendar-card">
        <div className="calendar-header">
          <button onClick={prevMonth}>‹</button>

          <h2>
            {year}년 {month}월
          </h2>

          <button onClick={nextMonth}>›</button>
        </div>

        <div className="calendar-weekdays">
          <span>일</span>
          <span>월</span>
          <span>화</span>
          <span>수</span>
          <span>목</span>
          <span>금</span>
          <span>토</span>
        </div>

        {loading ? (
          <p className="calendar-loading">불러오는 중...</p>
        ) : (
          <div className="calendar-grid">{calendarCells}</div>
        )}
      </section>

      {/* =======================
          월간 요약
      ======================== */}

      {monthlyStats && (
        <section className="monthly-summary">
          <h2>{monthlyStats.month}월 요약</h2>

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
          </div>
        </section>
      )}

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

                      `${props.payload.emoji} ${name}`,
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

                    borderLeft: `5px solid ${emotion.fill}`,
                  }}
                >
                  <div className="emotion-legend-left">
                    <span className="emotion-legend-emoji">
                      {emotion.emoji}
                    </span>

                    <div>
                      <strong
                        style={{
                          color: emotion.text,
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
                      color: emotion.text,
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
            </button>
          </div>

          <div className="detail-score">
            <span>
              기분 점수
              <strong>{selectedEntry.moodScore}점</strong>
            </span>

            <span>
              감정 강도
              <strong>{selectedEntry.intensity}</strong>
            </span>
          </div>

          <div className="detail-content">
            <span>오늘의 기록</span>

            <p>{selectedEntry.diaryContent || "작성된 내용이 없습니다."}</p>
          </div>
        </section>
      )}

      {/* =======================
          행동 전 · 후 그래프
      ======================== */}

      {moodChartData.length > 0 && (
        <section className="mood-chart-section">
          <h2>{month}월 행동 전·후 기분 변화</h2>

          <div className="mood-chart">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={moodChartData}>
                <CartesianGrid strokeDasharray="3 3" />

                <XAxis dataKey="date" />

                <YAxis domain={[0, 60]} ticks={[0, 15, 30, 45, 60]} />

                <Tooltip formatter={(value, name) => [`${value}점`, name]} />

                <Legend />

                {/* 행동 전 */}

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

                {/* 행동 후 */}

                <Line
                  type="monotone"
                  dataKey="afterScore"
                  name="행동 후"
                  stroke="#22c55e"
                  strokeWidth={3}
                  dot={{
                    r: 4,
                    strokeWidth: 3,
                  }}
                  activeDot={{
                    r: 6,
                  }}
                  connectNulls={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
      )}
    </main>
  );
}
